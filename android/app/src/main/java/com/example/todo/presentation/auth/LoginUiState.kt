package com.example.todo.presentation.auth

/**
 * Theo LLD mục 4.2 (nguyên văn).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val generalError: String? = null,
    val loginSuccess: Boolean = false
)
