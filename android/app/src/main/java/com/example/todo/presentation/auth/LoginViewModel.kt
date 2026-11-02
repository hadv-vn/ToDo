package com.example.todo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.auth.LoginUseCase
import com.example.todo.domain.usecase.auth.ResendVerificationUseCase
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
 *
 * auth-core trả `EMAIL_NOT_VERIFIED` (qua `ApiResult.Error.errorCode`) khi user đăng ký nhưng
 * chưa xác thực email -> hiện nút "Gửi lại email xác thực" gọi [ResendVerificationUseCase].
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val resendVerificationUseCase: ResendVerificationUseCase
) : ViewModel() {

    private companion object {
        const val EMAIL_NOT_VERIFIED_CODE = "EMAIL_NOT_VERIFIED"
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value, emailError = null, generalError = null, showResendVerification = false)
        }
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

        _uiState.update { it.copy(isSubmitting = true, generalError = null, infoMessage = null) }
        viewModelScope.launch {
            val result = loginUseCase(state.email.trim(), state.password)
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, loginSuccess = true) }
                }
                is ApiResult.Error -> {
                    val isEmailNotVerified = result.errorCode == EMAIL_NOT_VERIFIED_CODE
                    val message = when {
                        isEmailNotVerified -> result.message ?: "Email chưa được xác thực"
                        result.code == ApiErrorCode.UNAUTHORIZED -> "Email hoặc mật khẩu không đúng"
                        else -> result.message ?: "Đã có lỗi xảy ra, vui lòng thử lại"
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            generalError = message,
                            showResendVerification = isEmailNotVerified
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(isSubmitting = false, generalError = "Không có kết nối mạng, vui lòng thử lại")
                    }
                }
            }
        }
    }

    fun onResendVerification() {
        val state = _uiState.value
        if (state.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true, generalError = null, infoMessage = null) }
        viewModelScope.launch {
            val result = resendVerificationUseCase(state.email.trim())
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showResendVerification = false,
                            infoMessage = "Nếu email tồn tại và chưa xác thực, một email xác thực mới đã được gửi."
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isSubmitting = false, generalError = result.message ?: "Đã có lỗi xảy ra, vui lòng thử lại")
                    }
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
