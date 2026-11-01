package com.example.todo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Theo LLD mục 4.2/6.1. Validate inline đồng bộ (non-empty) TRƯỚC khi gọi [LoginUseCase] —
 * KHÔNG gọi API khi invalid.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val emailError = if (state.email.isBlank()) "Email không được để trống" else null
        val passwordError = if (state.password.isBlank()) "Mật khẩu không được để trống" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, generalError = null) }
        viewModelScope.launch {
            val result = loginUseCase(state.email.trim(), state.password)
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, loginSuccess = true) }
                }
                is ApiResult.Error -> {
                    val message = when (result.code) {
                        ApiErrorCode.UNAUTHORIZED -> "Email hoặc mật khẩu không đúng"
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
