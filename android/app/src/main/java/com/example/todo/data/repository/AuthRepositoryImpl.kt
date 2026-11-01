package com.example.todo.data.repository

import com.example.todo.data.local.TokenLocalDataSource
import com.example.todo.data.mapper.toDomain
import com.example.todo.data.remote.api.AuthApiService
import com.example.todo.data.remote.dto.auth.AuthResponseDto
import com.example.todo.data.remote.dto.auth.ForgotPasswordRequestDto
import com.example.todo.data.remote.dto.auth.LoginRequestDto
import com.example.todo.data.remote.dto.auth.RegisterRequestDto
import com.example.todo.data.remote.dto.auth.ResetPasswordRequestDto
import com.example.todo.data.remote.safeApiCall
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Implementation thật của [AuthRepository] — theo LLD mục 3.5.
 * register/login thành công -> lưu token qua [TokenLocalDataSource] TRƯỚC khi trả [ApiResult.Success].
 * logout(): chỉ xóa token cục bộ (không có API /auth/logout).
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenLocalDataSource: TokenLocalDataSource
) : AuthRepository {

    override suspend fun register(
        email: String,
        password: String,
        confirmPassword: String
    ): ApiResult<AuthSession> {
        val result = safeApiCall {
            authApiService.register(
                RegisterRequestDto(email = email, password = password, confirmPassword = confirmPassword)
            )
        }
        return persistSessionIfSuccess(result)
    }

    override suspend fun login(email: String, password: String): ApiResult<AuthSession> {
        val result = safeApiCall {
            authApiService.login(LoginRequestDto(email = email, password = password))
        }
        return persistSessionIfSuccess(result)
    }

    override suspend fun logout() {
        tokenLocalDataSource.clearToken()
    }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> {
        return safeApiCall { authApiService.forgotPassword(ForgotPasswordRequestDto(email = email)) }
    }

    override suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmNewPassword: String
    ): ApiResult<Unit> {
        return safeApiCall {
            authApiService.resetPassword(
                ResetPasswordRequestDto(
                    email = email,
                    otp = otp,
                    newPassword = newPassword,
                    confirmNewPassword = confirmNewPassword
                )
            )
        }
    }

    override suspend fun hasStoredToken(): Boolean = tokenLocalDataSource.hasToken()

    private suspend fun persistSessionIfSuccess(result: ApiResult<AuthResponseDto>): ApiResult<AuthSession> {
        return when (result) {
            is ApiResult.Success -> {
                tokenLocalDataSource.saveToken(result.data.accessToken)
                ApiResult.Success(
                    AuthSession(accessToken = result.data.accessToken, user = result.data.user.toDomain())
                )
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }
}
