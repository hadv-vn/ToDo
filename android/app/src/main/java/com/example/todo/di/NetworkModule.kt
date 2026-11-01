package com.example.todo.di

import com.example.todo.BuildConfig
import com.example.todo.data.remote.api.AuthApiService
import com.example.todo.data.remote.api.TaskApiService
import com.example.todo.data.remote.interceptor.AuthHeaderInterceptor
import com.example.todo.data.remote.interceptor.SessionExpiredInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Cung cấp OkHttpClient + Retrofit qua Hilt — theo LLD mục 3.1/3.7 và HLD mục 2.
 *
 * Phase 1: thêm AuthHeaderInterceptor/SessionExpiredInterceptor (401 toàn cục), AuthApiService,
 * appScope sống cùng Application (dùng bởi SessionExpiredInterceptor để launch coroutine xử lý
 * suspend fun từ trong intercept() không-suspend). TaskApiService thuộc Phase 2.
 *
 * Thứ tự interceptor: loggingInterceptor được add TRƯỚC authHeaderInterceptor để log ghi lại
 * request ở dạng CHƯA có header Authorization (giảm rủi ro JWT bị in ra Logcat ở build debug).
 *
 * Phase 2: thêm TaskApiService (nhóm /tasks, có Authorization qua AuthHeaderInterceptor).
 *
 * BASE_URL đọc theo build variant qua BuildConfig (xem app/build.gradle.kts):
 * - debug: "http://10.0.2.2:3000/" (alias loopback của emulator Android tới localhost máy host).
 * - release: TODO, cần chốt domain + TLS production trước khi build release thật.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Sống cùng Application, dùng cho mọi coroutine không gắn với vòng đời UI (ví dụ
    // SessionExpiredInterceptor cần launch suspend fun từ intercept() không phải suspend).
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authHeaderInterceptor: AuthHeaderInterceptor,
        sessionExpiredInterceptor: SessionExpiredInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(sessionExpiredInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideTaskApiService(retrofit: Retrofit): TaskApiService = retrofit.create(TaskApiService::class.java)
}
