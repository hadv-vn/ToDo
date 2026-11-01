package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * Body POST /auth/register — field name theo API_Contract_ToDo_Android.md mục 2.1.
 */
@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val confirmPassword: String
)
