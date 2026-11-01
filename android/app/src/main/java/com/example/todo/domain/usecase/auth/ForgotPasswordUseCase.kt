package com.example.todo.domain.usecase.auth

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — validate email format TRƯỚC khi gọi Repository.
 */
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): ApiResult<Unit> {
        if (!AuthValidationRules.isValidEmail(email)) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Email không đúng định dạng")
        }
        return authRepository.forgotPassword(email.trim())
    }
}
