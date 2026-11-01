package com.example.todo.presentation.auth

/**
 * Theo LLD mục 4.2 (nguyên văn).
 */
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSubmitting: Boolean = false,
    val generalError: String? = null,
    val registerSuccess: Boolean = false
)
