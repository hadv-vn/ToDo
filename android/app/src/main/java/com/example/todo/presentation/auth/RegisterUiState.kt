package com.example.todo.presentation.auth

/**
 * Theo LLD mục 4.2 (nguyên văn), cập nhật cho auth-core: đăng ký KHÔNG tự động đăng nhập (bắt
 * buộc xác thực email trước) -> `registerSuccess` chỉ dùng để khóa form + hiện `successMessage`,
 * KHÔNG điều hướng tự động vào app.
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
    val registerSuccess: Boolean = false,
    val successMessage: String? = null
)
