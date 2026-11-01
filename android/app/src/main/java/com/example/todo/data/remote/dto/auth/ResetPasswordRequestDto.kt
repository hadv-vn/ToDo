package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * Body POST /auth/reset-password — field name theo API_Contract_ToDo_Android.md mục 2.4.
 */
@Serializable
data class ResetPasswordRequestDto(
    val email: String,
    val otp: String,
    val newPassword: String,
    val confirmNewPassword: String
)
