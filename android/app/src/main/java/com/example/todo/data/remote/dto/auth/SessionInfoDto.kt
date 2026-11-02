package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * `session` object trong `LoginResultDto.data.session` — theo auth-core
 * `AuthService.login` (`LoginResult.session: { id, expiresAt }`). Session THẬT nằm trong cookie
 * httpOnly `sid` (OkHttp CookieJar tự lưu) — field này chỉ để hiển thị/log, KHÔNG dùng làm token.
 */
@Serializable
data class SessionInfoDto(
    val id: String,
    val expiresAt: String
)
