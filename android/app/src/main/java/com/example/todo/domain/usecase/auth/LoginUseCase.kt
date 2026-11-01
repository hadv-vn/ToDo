package com.example.todo.domain.usecase.auth

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — chỉ check non-empty (login KHÔNG enforce format email, tránh lộ thông tin
 * email có hợp lệ format hay không khác với có tồn tại hay không — API_Contract mục 2.2).
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): ApiResult<AuthSession> {
        if (email.isBlank()) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Email không được để trống")
        }
        if (password.isBlank()) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Mật khẩu không được để trống")
        }
        return authRepository.login(email, password)
    }
}
