package com.example.todo.domain.usecase.auth

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — otp non-empty, password rule, confirm khớp — validate TRƯỚC khi gọi Repository.
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        otp: String,
        newPassword: String,
        confirmNewPassword: String
    ): ApiResult<Unit> {
        if (otp.isBlank()) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Mã xác nhận không được để trống")
        }
        if (!AuthValidationRules.isValidOtp(otp)) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Mã xác nhận phải gồm đúng 6 chữ số")
        }
        if (!AuthValidationRules.isValidPassword(newPassword)) {
            return ApiResult.Error(
                ApiErrorCode.BAD_REQUEST,
                "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 chữ và 1 số"
            )
        }
        if (confirmNewPassword != newPassword) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Xác nhận mật khẩu không khớp")
        }
        return authRepository.resetPassword(email, otp, newPassword, confirmNewPassword)
    }
}
