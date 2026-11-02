package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/** Body `POST /api/v1/auth/resend-verification`. */
@Serializable
data class ResendVerificationRequestDto(
    val email: String
)
