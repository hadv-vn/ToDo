package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/** Response 201 của `POST /api/v1/auth/register` — auth-core bọc `{ data, message }`. */
@Serializable
data class RegisterResponseDto(
    val data: RegisteredUserDto,
    val message: String
)
