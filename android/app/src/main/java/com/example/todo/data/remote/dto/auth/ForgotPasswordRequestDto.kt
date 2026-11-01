package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * Body POST /auth/forgot-password — field name theo API_Contract_ToDo_Android.md mục 2.3.
 */
@Serializable
data class ForgotPasswordRequestDto(
    val email: String
)
