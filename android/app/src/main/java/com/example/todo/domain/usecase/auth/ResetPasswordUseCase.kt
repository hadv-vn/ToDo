package com.example.todo.domain.usecase.auth

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — token non-empty, password rule, confirm khớp — validate TRƯỚC khi gọi
 * Repository.
 *
 * `token` lấy từ link email (auth-core reset-password dùng token opaque, KHÔNG phải OTP 6 số) —
 * không có format cố định để validate ngoài "không rỗng". `confirmNewPassword` CHỈ validate ở
 * client — auth-core `ResetPasswordDto` không nhận field này.
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        token: String,
        newPassword: String,
        confirmNewPassword: String
    ): ApiResult<Unit> {
        if (token.isBlank()) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Vui lòng nhập token xác nhận từ email")
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
        return authRepository.resetPassword(token.trim(), newPassword)
    }
}
