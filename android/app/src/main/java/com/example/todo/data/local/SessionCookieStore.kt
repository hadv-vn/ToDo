package com.example.todo.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.IOException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * `auth-core` xác thực qua session cookie httpOnly (`sid`) + cookie CSRF
 * (`csrfToken`) do server set qua `Set-Cookie` — KHÔNG còn Bearer JWT (khác
 * backend Express cũ). [CookieJar] này đóng vai trò lưu trữ cookie cho
 * OkHttp (tự động gắn `Cookie:` header cho mọi request tới cùng host), kèm
 * mã hóa khi persist — tái dùng đúng kỹ thuật AES/GCM + `AndroidKeyStore`
 * trước đây dùng cho JWT (xem lịch sử `TokenLocalDataSource`).
 *
 * Chỉ nói tới ĐÚNG 1 backend host (BASE_URL) trong toàn app -> không cần xử
 * lý multi-domain/subdomain đầy đủ như OkHttp `Cookie` hỗ trợ; mọi cookie
 * lưu dưới dạng host-only, match bằng [Cookie.matches] chuẩn OkHttp.
 */
interface SessionCookieStore : CookieJar {
    /** Có cookie session (`sid`) còn hạn hay không — dùng cho "giữ phiên". */
    suspend fun hasActiveSessionCookie(): Boolean

    /** Xóa toàn bộ cookie đã lưu (logout / phát hiện 401 toàn cục). */
    suspend fun clear()
}

class SessionCookieStoreImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) : SessionCookieStore {

    companion object {
        private val KEY_ENCRYPTED_COOKIES = stringPreferencesKey("encrypted_session_cookies")
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
        // FIELD_SEPARATOR/COOKIE_SEPARATOR là ký tự control (U+0001/U+0002) KHÔNG hiển thị khi
        // xem file — KHÔNG phải chuỗi rỗng. Chỉ dùng nội bộ serialize/deserialize, không bao giờ
        // xuất hiện trong tên/giá trị cookie thật (base64url) nên an toàn làm delimiter.
        private const val FIELD_SEPARATOR = ""
        private const val COOKIE_SEPARATOR = ""
        private const val SESSION_COOKIE_NAME = "sid"
    }

    // Xem TokenLocalDataSource (lịch sử) — cùng kỹ thuật MasterKey + AndroidKeyStore.
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(MasterKey.DEFAULT_MASTER_KEY_ALIAS, null) as SecretKey
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherText
        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encoded: String): String? {
        return try {
            val combined = Base64.getDecoder().decode(encoded)
            if (combined.size <= GCM_IV_LENGTH_BYTES) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherText = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            // Hỏng/không giải mã được -> coi như không có cookie nào, KHÔNG throw (buộc đăng nhập
            // lại là xử lý hợp lý, an toàn — cùng nguyên tắc TokenLocalDataSource lịch sử).
            null
        }
    }

    private fun serialize(cookies: List<Cookie>): String = cookies.joinToString(COOKIE_SEPARATOR) { cookie ->
        listOf(
            cookie.name,
            cookie.value,
            cookie.domain,
            cookie.path,
            cookie.expiresAt.toString(),
            cookie.secure.toString(),
            cookie.httpOnly.toString()
        ).joinToString(FIELD_SEPARATOR)
    }

    private fun deserialize(serialized: String): List<Cookie> {
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(COOKIE_SEPARATOR).mapNotNull { line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != 7) return@mapNotNull null
            val (name, value, domain, path, expiresAt, secure, httpOnly) = fields
            val builder = Cookie.Builder()
                .name(name)
                .value(value)
                .domain(domain)
                .path(path)
                .expiresAt(expiresAt.toLongOrNull() ?: return@mapNotNull null)
            if (secure.toBoolean()) builder.secure()
            if (httpOnly.toBoolean()) builder.httpOnly()
            builder.build()
        }
    }

    // destructuring operator cho List<String> 7 phần tử — tiện đọc ở deserialize().
    private operator fun List<String>.component6(): String = this[5]
    private operator fun List<String>.component7(): String = this[6]

    @Volatile
    private var cache: List<Cookie>? = null

    private fun loadCache(): List<Cookie> {
        val existing = cache
        if (existing != null) return existing
        // CookieJar.loadForRequest/saveFromResponse chạy trên thread dispatcher của OkHttp (KHÔNG
        // phải main thread) -> buộc phải block thread hiện tại để đọc lần đầu, cùng cách
        // TokenLocalDataSourceImpl.getTokenBlocking() lịch sử đã làm.
        val loaded = runBlocking(Dispatchers.IO) { readPersistedCookies() }
        cache = loaded
        return loaded
    }

    private suspend fun readPersistedCookies(): List<Cookie> {
        val encrypted = dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs -> prefs[KEY_ENCRYPTED_COOKIES] }
            .first() ?: return emptyList()
        val serialized = decrypt(encrypted) ?: return emptyList()
        return deserialize(serialized)
    }

    private fun persist(cookies: List<Cookie>) {
        cache = cookies
        runBlocking(Dispatchers.IO) {
            val encrypted = encrypt(serialize(cookies))
            dataStore.edit { prefs -> prefs[KEY_ENCRYPTED_COOKIES] = encrypted }
        }
    }

    private fun Cookie.isExpired(now: Long = System.currentTimeMillis()): Boolean = expiresAt <= now

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val merged = loadCache().toMutableList()
        for (cookie in cookies) {
            merged.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            if (!cookie.isExpired()) merged.add(cookie)
        }
        persist(merged.filterNot { it.isExpired() })
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return loadCache().filter { !it.isExpired(now) && it.matches(url) }
    }

    override suspend fun hasActiveSessionCookie(): Boolean {
        return withContext(Dispatchers.IO) {
            readPersistedCookies().any { it.name == SESSION_COOKIE_NAME && !it.isExpired() }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs -> prefs.remove(KEY_ENCRYPTED_COOKIES) }
        }
        cache = emptyList()
    }
}
