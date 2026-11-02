package com.example.todo.data.remote.api

import com.example.todo.data.remote.dto.auth.ForgotPasswordRequestDto
import com.example.todo.data.remote.dto.auth.LoginRequestDto
import com.example.todo.data.remote.dto.auth.LoginResponseDto
import com.example.todo.data.remote.dto.auth.RegisterRequestDto
import com.example.todo.data.remote.dto.auth.RegisterResponseDto
import com.example.todo.data.remote.dto.auth.ResendVerificationRequestDto
import com.example.todo.data.remote.dto.auth.ResetPasswordRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface cho nhóm endpoint `api/v1/auth` của auth-core. KHÔNG cần CSRF header (chưa
 * có session) — [com.example.todo.data.remote.interceptor.CsrfHeaderInterceptor] tự loại trừ
 * path này. Session thật xác lập qua cookie `Set-Cookie` (OkHttp CookieJar tự lưu), KHÔNG qua
 * response body.
 */
interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<RegisterResponseDto>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<LoginResponseDto>

    @POST("api/v1/auth/resend-verification")
    suspend fun resendVerification(@Body body: ResendVerificationRequestDto): Response<Unit>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): Response<Unit>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): Response<Unit>

    // Cần session cookie hợp lệ (AuthGuard) nhưng KHÔNG cần CSRF — auth-core AuthController.logout
    // chỉ @UseGuards(AuthGuard), không có CsrfGuard (chấp nhận rủi ro login-CSRF thấp, cho phép
    // logout dù client chưa từng có cookie CSRF).
    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>
}
