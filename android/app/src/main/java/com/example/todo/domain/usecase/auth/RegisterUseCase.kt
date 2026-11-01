package com.example.todo.domain.usecase.auth

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — validate email format/password rule/confirm khớp TRƯỚC khi gọi Repository;
 * invalid -> ApiResult.Error(BAD_REQUEST,...) ngay, KHÔNG gọi network. Validate ở đây mang tính
 * defensive (bảo vệ invariant nghiệp vụ bất kể entry point nào gọi) — ViewModel validate lại lần
 * đầu để hiển thị lỗi inline tức thời, không phải trùng lặp vô ích (xem LLD mục 4.1 cuối).
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String
    ): ApiResult<AuthSession> {
        if (!AuthValidationRules.isValidEmail(email)) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Email không đúng định dạng")
        }
        if (!AuthValidationRules.isValidPassword(password)) {
            return ApiResult.Error(
                ApiErrorCode.BAD_REQUEST,
                "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 chữ và 1 số"
            )
        }
        if (confirmPassword != password) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Xác nhận mật khẩu không khớp")
        }
        return authRepository.register(email.trim(), password, confirmPassword)
    }
}
