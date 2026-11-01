package com.example.todo.data.remote.api

import com.example.todo.data.remote.dto.auth.AuthResponseDto
import com.example.todo.data.remote.dto.auth.ForgotPasswordRequestDto
import com.example.todo.data.remote.dto.auth.LoginRequestDto
import com.example.todo.data.remote.dto.auth.RegisterRequestDto
import com.example.todo.data.remote.dto.auth.ResetPasswordRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface cho nhóm endpoint /auth — path/method theo API_Contract_ToDo_Android.md mục 2.
 * Base path không có prefix version (mục 1.1), KHÔNG cần Authorization header (mục 1.2).
 */
interface AuthApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<AuthResponseDto>

    // Response thật có body {"message": "..."} (mục 2.3) nhưng không cần dùng tới ở Android ->
    // khai báo Response<Unit>, Json converter (ignoreUnknownKeys=true) bỏ qua field thừa khi decode.
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): Response<Unit>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): Response<Unit>
}
