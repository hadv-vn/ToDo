package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * Body `POST /api/v1/auth/reset-password` — theo auth-core `ResetPasswordDto`
 * (`token` lấy từ link email, KHÔNG phải OTP/email/confirmNewPassword — server suy ra user từ
 * `token`, confirm chỉ validate ở client).
 */
@Serializable
data class ResetPasswordRequestDto(
    val token: String,
    val newPassword: String
)
