package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * Body POST /auth/login — field name theo API_Contract_ToDo_Android.md mục 2.2.
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)
