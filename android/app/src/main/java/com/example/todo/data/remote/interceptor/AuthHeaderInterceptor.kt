package com.example.todo.data.remote.interceptor

import com.example.todo.data.local.TokenLocalDataSource
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Gắn header `Authorization: Bearer <token>` CHỈ cho request có path bắt đầu `/tasks` — theo LLD
 * mục 3.7. Các endpoint `/auth` không cần header này (API_Contract mục 1.2).
 *
 * `intercept()` chạy trên thread dispatcher của OkHttp (KHÔNG phải main thread, KHÔNG phải suspend
 * fun) -> đọc token qua [TokenLocalDataSource.getTokenBlocking] (đồng bộ).
 */
class AuthHeaderInterceptor @Inject constructor(
    private val tokenLocalDataSource: TokenLocalDataSource
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (!path.startsWith("/tasks")) {
            return chain.proceed(original)
        }
        val token = tokenLocalDataSource.getTokenBlocking()
        val request = if (token != null) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
