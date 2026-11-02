package com.example.todo.presentation.auth

/**
 * Theo LLD mục 4.2 (nguyên văn), bổ sung `showResendVerification`/`infoMessage` cho luồng
 * `EMAIL_NOT_VERIFIED` của auth-core (xem [LoginViewModel]).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val generalError: String? = null,
    val loginSuccess: Boolean = false,
    val showResendVerification: Boolean = false,
    val infoMessage: String? = null
)
