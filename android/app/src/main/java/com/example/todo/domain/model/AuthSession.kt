package com.example.todo.domain.model

/**
 * Domain Entity AuthSession — theo LLD mục 3.2.
 *
 * KHÔNG còn `accessToken`: auth-core xác thực qua session cookie httpOnly do server quản lý
 * (OkHttp CookieJar tự lưu/gắn), Domain/Presentation không cần biết hay giữ token nào.
 */
data class AuthSession(
    val user: AuthUser
)
