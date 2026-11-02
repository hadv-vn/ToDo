package com.example.todo.presentation.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.auth.AuthValidationRules
import com.example.todo.domain.usecase.auth.ResetPasswordUseCase
import com.example.todo.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Theo LLD mục 4.2/6.2. Nhận `email` từ nav argument của route `reset_password/{email}` qua
 * [SavedStateHandle] (Hilt Navigation Compose tự gắn arguments của NavBackStackEntry vào
 * SavedStateHandle khi dùng hiltViewModel() trong 1 composable() của NavHost) — CHỈ để hiển thị,
 * auth-core `reset-password` suy ra user từ `token`, không cần `email` trong body request.
 */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val email: String = savedStateHandle.get<String>(Routes.RESET_PASSWORD_ARG_EMAIL).orEmpty()

    private val _uiState = MutableStateFlow(ResetPasswordUiState(email = email))
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(token = value, tokenError = null, generalError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null, generalError = null) }
    }

    fun onConfirmNewPasswordChange(value: String) {
        _uiState.update { it.copy(confirmNewPassword = value, confirmNewPasswordError = null, generalError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val tokenError = if (state.token.isBlank()) "Vui lòng nhập token xác nhận từ email" else null
        val newPasswordError = if (!AuthValidationRules.isValidPassword(state.newPassword)) {
            "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 chữ và 1 số"
        } else {
            null
        }
        val confirmNewPasswordError = if (state.confirmNewPassword != state.newPassword) {
            "Xác nhận mật khẩu không khớp"
        } else {
            null
        }
        if (tokenError != null || newPasswordError != null || confirmNewPasswordError != null) {
            _uiState.update {
                it.copy(
                    tokenError = tokenError,
                    newPasswordError = newPasswordError,
                    confirmNewPasswordError = confirmNewPasswordError
                )
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, generalError = null) }
        viewModelScope.launch {
            val result = resetPasswordUseCase(state.token, state.newPassword, state.confirmNewPassword)
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, resetSuccess = true) }
                }
                is ApiResult.Error -> {
                    val message = when (result.code) {
                        ApiErrorCode.BAD_REQUEST -> result.message ?: "Token không đúng hoặc đã hết hạn"
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
