package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * `user` object trong `LoginResultDto.data.user` — theo auth-core
 * `AuthController.login`/`AuthService.login` (`LoginResult.user: { id, email, role }`).
 */
@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    val role: String
)
