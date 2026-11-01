package com.example.todo.domain.model

/**
 * Domain Entity AuthSession — theo LLD mục 3.2.
 */
data class AuthSession(
    val accessToken: String,
    val user: AuthUser
)
