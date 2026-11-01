package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * Response 200/201 của POST /auth/register và /auth/login — theo API_Contract_ToDo_Android.md
 * mục 2.1/2.2.
 */
@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val user: UserDto
)
