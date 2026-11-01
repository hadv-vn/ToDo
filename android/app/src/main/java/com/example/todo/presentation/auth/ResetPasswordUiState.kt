package com.example.todo.presentation.auth

/**
 * Theo LLD mục 4.2 (nguyên văn). `email` nhận từ nav arg (ForgotPasswordScreen truyền sang,
 * đọc qua SavedStateHandle ở [ResetPasswordViewModel]).
 */
data class ResetPasswordUiState(
    val email: String,
    val otp: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val otpError: String? = null,
    val newPasswordError: String? = null,
    val confirmNewPasswordError: String? = null,
    val isSubmitting: Boolean = false,
    val generalError: String? = null,
    val resetSuccess: Boolean = false
)
