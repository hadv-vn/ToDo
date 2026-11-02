package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * `data` object của response `POST /api/v1/auth/register` — theo auth-core
 * `AuthService.register` (`RegisteredUserResult`). User ở trạng thái `PENDING_VERIFICATION`,
 * CHƯA đăng nhập được tới khi xác thực email (`EmailNotVerifiedException` khi login).
 */
@Serializable
data class RegisteredUserDto(
    val id: String,
    val email: String,
    val role: String,
    val status: String,
    val createdAt: String
)
