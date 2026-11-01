package com.example.todo.domain.repository

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession

/**
 * Repository interface cho Auth — theo LLD mục 3.4.
 * Implementation thật (AuthRepositoryImpl) thuộc Phase 1, KHÔNG implement ở Phase 0.
 */
interface AuthRepository {
    suspend fun register(email: String, password: String, confirmPassword: String): ApiResult<AuthSession>
    suspend fun login(email: String, password: String): ApiResult<AuthSession>

    // Chỉ xóa token cục bộ, KHÔNG gọi API (không có /auth/logout).
    suspend fun logout()

    suspend fun forgotPassword(email: String): ApiResult<Unit>
    suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmNewPassword: String
    ): ApiResult<Unit>

    // Kiểm tra nhanh có token lưu cục bộ hay không (dùng cho "giữ phiên").
    suspend fun hasStoredToken(): Boolean
}
