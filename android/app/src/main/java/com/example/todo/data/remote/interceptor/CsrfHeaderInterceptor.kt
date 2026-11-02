package com.example.todo.data.remote.interceptor

import com.example.todo.data.local.SessionCookieStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * `auth-core` dùng CSRF double-submit cookie (`csrfToken`, KHÔNG httpOnly) + header
 * `x-csrf-token` cho mọi request thay đổi dữ liệu trên route đã đăng nhập — xem
 * `auth-core/backend/src/common/csrf/csrf.guard.ts`/`csrf.constants.ts`. Nhóm endpoint `auth`
 * (register/login/forgot/reset-password) KHÔNG cần (chưa có session) nên loại trừ riêng.
 *
 * Chạy trên thread dispatcher của OkHttp (KHÔNG phải main thread) -> đọc cookie qua
 * [SessionCookieStore.loadForRequest] (đồng bộ, giống [AuthHeaderInterceptor] lịch sử đọc token).
 */
class CsrfHeaderInterceptor @Inject constructor(
    private val sessionCookieStore: SessionCookieStore
) : Interceptor {

    companion object {
        private const val CSRF_COOKIE_NAME = "csrfToken"
        private const val CSRF_HEADER_NAME = "x-csrf-token"
        private val MUTATING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private const val AUTH_PATH_PREFIX = "/api/v1/auth/"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val needsCsrf = original.method in MUTATING_METHODS &&
            !original.url.encodedPath.startsWith(AUTH_PATH_PREFIX)
        if (!needsCsrf) {
            return chain.proceed(original)
        }

        val csrfToken = sessionCookieStore.loadForRequest(original.url)
            .firstOrNull { it.name == CSRF_COOKIE_NAME }
            ?.value

        val request = if (csrfToken != null) {
            original.newBuilder().addHeader(CSRF_HEADER_NAME, csrfToken).build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
