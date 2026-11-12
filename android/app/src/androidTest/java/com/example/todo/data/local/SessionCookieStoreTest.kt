package com.example.todo.data.local

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test
import kotlin.random.Random

/**
 * Instrumented test cho [SessionCookieStoreImpl] — PHẢI chạy trên thiết bị/emulator thật vì dùng
 * AndroidKeyStore thật (không mock được qua Robolectric/JVM unit test) để mã hóa cookie trước khi
 * persist vào DataStore.
 *
 * Trước đây [SessionCookieStoreImpl.getSecretKey] gọi `keyStore.getKey(MasterKey.DEFAULT_MASTER_KEY_ALIAS,
 * null)` TRỰC TIẾP mà không "chạm" [androidx.security.crypto.MasterKey] trước -> trên thiết bị
 * mới/app data vừa bị xóa (alias chưa từng được tạo trong AndroidKeyStore), `getKey()` trả `null`
 * -> `ClassCastException` (`null as SecretKey`) ngay lần đầu `saveFromResponse` được gọi với cookie
 * thật (vd ngay sau khi đăng nhập lần đầu). Test dưới đây exercise đúng code path đó
 * (saveFromResponse -> persist -> encrypt -> getSecretKey) để làm regression test cho bug này.
 *
 * Mỗi test tự tạo DataStore riêng (tên file ngẫu nhiên) để các test không chia sẻ state với nhau
 * hay với DataStore thật của app (`todo_secure_prefs` khai báo ở [com.example.todo.di.DataStoreModule]).
 */
class SessionCookieStoreTest {

    private fun newStore(): SessionCookieStoreImpl {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileName = "session_cookie_store_test_${Random.nextLong()}"
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(fileName) }
        )
        return SessionCookieStoreImpl(context, dataStore)
    }

    private val url = "http://10.0.2.2:8081/api/v1/tasks".toHttpUrl()

    private fun sidCookie(value: String) = Cookie.Builder()
        .name("sid")
        .value(value)
        .domain(url.host)
        .path("/")
        .expiresAt(System.currentTimeMillis() + 60_000)
        .build()

    @Test
    fun saveFromResponse_maHoaRoiDocLaiDungGiaTri() {
        val store = newStore()

        // Trước fix: ClassCastException (null as SecretKey) ngay ở đây trên thiết bị/app data mới.
        store.saveFromResponse(url, listOf(sidCookie("session-token-123")))

        val loaded = store.loadForRequest(url)
        assert(loaded.any { it.name == "sid" && it.value == "session-token-123" }) {
            "Cookie 'sid' vừa lưu phải đọc lại được đúng giá trị sau khi mã hóa/giải mã"
        }
    }

    @Test
    fun hasActiveSessionCookie_coCookieSidConHan_traVeTrue() {
        val store = newStore()
        store.saveFromResponse(url, listOf(sidCookie("session-token-456")))

        val hasActive = runBlocking { store.hasActiveSessionCookie() }

        assert(hasActive) { "Phải có session cookie 'sid' còn hạn sau khi saveFromResponse" }
    }

    @Test
    fun clear_xoaHetCookie_hasActiveSessionCookieTraVeFalseVaLoadForRequestRong() {
        val store = newStore()
        store.saveFromResponse(url, listOf(sidCookie("session-token-789")))

        runBlocking { store.clear() }

        assert(store.loadForRequest(url).isEmpty()) { "loadForRequest phải rỗng sau clear()" }
        assert(!runBlocking { store.hasActiveSessionCookie() }) {
            "hasActiveSessionCookie phải false sau clear()"
        }
    }

    @Test
    fun cookieHetHan_khongDuocTraVeOLoadForRequest() {
        val store = newStore()
        val expired = Cookie.Builder()
            .name("sid")
            .value("expired-token")
            .domain(url.host)
            .path("/")
            .expiresAt(System.currentTimeMillis() - 1_000)
            .build()

        store.saveFromResponse(url, listOf(expired))

        assert(store.loadForRequest(url).isEmpty()) { "Cookie đã hết hạn không được trả về" }
    }
}
