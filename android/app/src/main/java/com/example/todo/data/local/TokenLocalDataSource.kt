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
import java.io.IOException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Lưu/đọc JWT an toàn — theo LLD mục "Android Data Layer"/HLD, dùng Jetpack DataStore
 * (Preferences) kết hợp androidx.security:security-crypto.
 *
 * `saveToken`/`getToken`/`clearToken`/`hasToken`: suspend, dùng ở Repository/UseCase.
 * `getTokenBlocking`: đọc đồng bộ, dùng bởi [com.example.todo.data.remote.interceptor.AuthHeaderInterceptor]
 * chạy trên thread dispatcher của OkHttp (KHÔNG phải main thread) — xem LLD mục 3.7.
 */
interface TokenLocalDataSource {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    fun getTokenBlocking(): String?
    suspend fun clearToken()
    suspend fun hasToken(): Boolean
}

class TokenLocalDataSourceImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) : TokenLocalDataSource {

    companion object {
        private val KEY_ENCRYPTED_TOKEN = stringPreferencesKey("encrypted_access_token")
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
    }

    /**
     * `MasterKey` (androidx.security.crypto) đảm bảo một key AES256_GCM được sinh/lưu trong
     * AndroidKeyStore (hardware-backed nếu thiết bị hỗ trợ) theo đúng tham số bảo mật khuyến nghị
     * của Jetpack Security — thay vì tự cấu hình `KeyGenParameterSpec` tay (dễ sai sót tham số).
     * Jetpack DataStore hiện chưa có biến thể "encrypted" chính thức nên token được tự mã hoá
     * bằng key này (AES/GCM qua JCA chuẩn) trước khi ghi vào DataStore — đây là cách "kết hợp"
     * DataStore + security-crypto mà vẫn chỉ dùng API public, ổn định của cả hai thư viện.
     */
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun getSecretKey(): SecretKey {
        // masterKey.keyAlias thật nhưng package-private trong androidx.security.crypto —
        // MasterKey.Builder không set alias tùy chỉnh nên key luôn được lưu dưới alias mặc định này.
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(MasterKey.DEFAULT_MASTER_KEY_ALIAS, null) as SecretKey
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // Lưu chung iv + ciphertext (iv không cần giữ bí mật, chỉ cần không tái sử dụng với cùng key).
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
            // Token hỏng/không giải mã được (đổi thiết bị, AndroidKeyStore bị reset...) -> coi như
            // không có token, KHÔNG throw lên tầng gọi (buộc đăng nhập lại là xử lý hợp lý, an toàn).
            null
        }
    }

    private suspend fun readEncryptedToken(): String? {
        return dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { prefs -> prefs[KEY_ENCRYPTED_TOKEN] }
            .first()
    }

    override suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            val encrypted = encrypt(token)
            dataStore.edit { prefs -> prefs[KEY_ENCRYPTED_TOKEN] = encrypted }
        }
    }

    override suspend fun getToken(): String? {
        return withContext(Dispatchers.IO) {
            readEncryptedToken()?.let { decrypt(it) }
        }
    }

    override fun getTokenBlocking(): String? {
        // Chạy trên thread dispatcher OkHttp (interceptor không phải suspend fun) -> buộc phải
        // block thread hiện tại, KHÔNG block main thread (xem AuthHeaderInterceptor).
        val encrypted = runBlocking(Dispatchers.IO) { readEncryptedToken() }
        return encrypted?.let { decrypt(it) }
    }

    override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs -> prefs.remove(KEY_ENCRYPTED_TOKEN) }
        }
    }

    override suspend fun hasToken(): Boolean {
        return getToken() != null
    }
}
