package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * Body `POST /api/v1/auth/register` — theo auth-core `RegisterDto` (chỉ `email`+`password`,
 * KHÔNG có `confirmPassword` — server không nhận field này; confirm chỉ validate ở client).
 */
@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String
)
