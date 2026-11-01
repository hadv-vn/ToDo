package com.example.todo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.auth.AuthValidationRules
import com.example.todo.domain.usecase.auth.ForgotPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Theo LLD mục 4.2/6.2. Validate inline đồng bộ (format email) TRƯỚC khi gọi
 * [ForgotPasswordUseCase] — KHÔNG gọi API khi invalid.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (!AuthValidationRules.isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Email không đúng định dạng") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, generalError = null) }
        viewModelScope.launch {
            val result = forgotPasswordUseCase(state.email.trim())
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, requestSent = true) }
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
