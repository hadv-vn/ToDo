package com.example.todo.presentation.auth

/**
 * Theo LLD mục 4.2, cập nhật cho auth-core: `token` (từ link email) thay cho `otp` (6 số) của
 * backend Express cũ. `email` nhận từ nav arg (ForgotPasswordScreen truyền sang, đọc qua
 * SavedStateHandle ở [ResetPasswordViewModel]) — chỉ dùng để HIỂN THỊ, không gửi lên server.
 */
data class ResetPasswordUiState(
    val email: String,
    val token: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val tokenError: String? = null,
    val newPasswordError: String? = null,
    val confirmNewPasswordError: String? = null,
    val isSubmitting: Boolean = false,
    val generalError: String? = null,
    val resetSuccess: Boolean = false
)
