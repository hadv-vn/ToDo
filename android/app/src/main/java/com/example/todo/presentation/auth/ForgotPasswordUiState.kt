package com.example.todo.presentation.auth

/**
 * Theo LLD mục 4.2 (nguyên văn). `requestSent = true` CHỈ khi backend thực sự phản hồi 200
 * (không phải network error).
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isSubmitting: Boolean = false,
    val generalError: String? = null,
    val requestSent: Boolean = false
)
