package com.example.todo.domain.usecase.auth

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Gửi lại email xác thực — dùng khi login báo `EMAIL_NOT_VERIFIED` (xem [LoginUseCase] và
 * `LoginViewModel`). Validate email format TRƯỚC khi gọi Repository, cùng rule với
 * [RegisterUseCase]/[ForgotPasswordUseCase].
 */
class ResendVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): ApiResult<Unit> {
        if (!AuthValidationRules.isValidEmail(email)) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Email không đúng định dạng")
        }
        return authRepository.resendVerification(email.trim())
    }
}
