package com.example.todo.domain.repository

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession

/**
 * Repository interface cho Auth — theo LLD mục 3.4.
 * Implementation thật (AuthRepositoryImpl) thuộc Phase 1, KHÔNG implement ở Phase 0.
 */
interface AuthRepository {
    // auth-core bắt buộc xác thực email trước khi login được -> KHÔNG trả AuthSession (chưa có
    // session nào được tạo ở bước đăng ký).
    suspend fun register(email: String, password: String): ApiResult<Unit>

    suspend fun login(email: String, password: String): ApiResult<AuthSession>

    // Gọi POST /auth/logout thật (auth-core có session server-side cần revoke), rồi luôn xóa
    // cookie session cục bộ dù API thành công hay lỗi.
    suspend fun logout()

    suspend fun resendVerification(email: String): ApiResult<Unit>

    suspend fun forgotPassword(email: String): ApiResult<Unit>

    // `token` lấy từ link email (auth-core reset-password dùng token, KHÔNG phải OTP).
    suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit>

    // Kiểm tra nhanh có session cookie còn hạn lưu cục bộ hay không (dùng cho "giữ phiên").
    suspend fun hasActiveSession(): Boolean
}
