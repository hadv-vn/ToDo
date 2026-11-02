package com.example.todo.data.repository

import com.example.todo.data.local.SessionCookieStore
import com.example.todo.data.mapper.toDomain
import com.example.todo.data.remote.api.AuthApiService
import com.example.todo.data.remote.dto.auth.ForgotPasswordRequestDto
import com.example.todo.data.remote.dto.auth.LoginRequestDto
import com.example.todo.data.remote.dto.auth.LoginResponseDto
import com.example.todo.data.remote.dto.auth.RegisterRequestDto
import com.example.todo.data.remote.dto.auth.ResendVerificationRequestDto
import com.example.todo.data.remote.dto.auth.ResetPasswordRequestDto
import com.example.todo.data.remote.safeApiCall
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Implementation thật của [AuthRepository] — theo LLD mục 3.5.
 *
 * Session của auth-core xác lập qua cookie `Set-Cookie` (httpOnly `sid`) — OkHttp [SessionCookieStore]
 * (gắn làm `CookieJar` ở NetworkModule) tự lưu/gắn, KHÔNG cần Repository tự đọc/lưu token nào.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionCookieStore: SessionCookieStore
) : AuthRepository {

    override suspend fun register(email: String, password: String): ApiResult<Unit> {
        val result = safeApiCall { authApiService.register(RegisterRequestDto(email = email, password = password)) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    override suspend fun login(email: String, password: String): ApiResult<AuthSession> {
        val result = safeApiCall { authApiService.login(LoginRequestDto(email = email, password = password)) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(toAuthSession(result.data))
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    override suspend fun logout() {
        // Session thật nằm phía server (Postgres) — PHẢI gọi API để revoke, không chỉ xóa cục bộ.
        // Luôn xóa cookie cục bộ dù API thành công/lỗi (vd mất mạng khi logout vẫn nên coi như đã
        // đăng xuất ở phía client).
        try {
            authApiService.logout()
        } catch (e: Exception) {
            // Mất mạng/lỗi khác khi logout — không chặn việc xóa session cục bộ.
        }
        sessionCookieStore.clear()
    }

    override suspend fun resendVerification(email: String): ApiResult<Unit> {
        return safeApiCall { authApiService.resendVerification(ResendVerificationRequestDto(email = email)) }
    }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> {
        return safeApiCall { authApiService.forgotPassword(ForgotPasswordRequestDto(email = email)) }
    }

    override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> {
        return safeApiCall {
            authApiService.resetPassword(ResetPasswordRequestDto(token = token, newPassword = newPassword))
        }
    }

    override suspend fun hasActiveSession(): Boolean = sessionCookieStore.hasActiveSessionCookie()

    private fun toAuthSession(loginResponse: LoginResponseDto): AuthSession =
        AuthSession(user = loginResponse.data.user.toDomain())
}
