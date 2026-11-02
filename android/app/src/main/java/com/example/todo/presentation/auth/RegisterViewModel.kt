package com.example.todo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.auth.AuthValidationRules
import com.example.todo.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Theo LLD mục 4.2/6.1. Validate inline đồng bộ (format email, password rule, confirm khớp)
 * TRƯỚC khi gọi [RegisterUseCase] — KHÔNG gọi API khi invalid.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, generalError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val emailError = if (!AuthValidationRules.isValidEmail(state.email)) {
            "Email không đúng định dạng"
        } else {
            null
        }
        val passwordError = if (!AuthValidationRules.isValidPassword(state.password)) {
            "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 chữ và 1 số"
        } else {
            null
        }
        val confirmPasswordError = if (state.confirmPassword != state.password) {
            "Xác nhận mật khẩu không khớp"
        } else {
            null
        }
        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, generalError = null) }
        viewModelScope.launch {
            val result = registerUseCase(state.email.trim(), state.password, state.confirmPassword)
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            registerSuccess = true,
                            successMessage = "Đăng ký thành công. Vui lòng kiểm tra email để xác thực " +
                                "tài khoản trước khi đăng nhập."
                        )
                    }
                }
                is ApiResult.Error -> {
                    val message = when (result.code) {
                        ApiErrorCode.CONFLICT -> "Email đã được sử dụng"
                        else -> result.message ?: "Đã có lỗi xảy ra, vui lòng thử lại"
                    }
                    _uiState.update { it.copy(isSubmitting = false, generalError = message) }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(isSubmitting = false, generalError = "Không có kết nối mạng, vui lòng thử lại")
                    }
                }
            }
        }
    }
}
