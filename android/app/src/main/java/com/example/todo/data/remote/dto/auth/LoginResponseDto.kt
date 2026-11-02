package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * Response 200 của `POST /api/v1/auth/login` — auth-core bọc mọi response thành công trong
 * `{ data: ... }` (khác bare JSON của backend Express cũ).
 */
@Serializable
data class LoginResponseDto(
    val data: LoginResultDto
)
