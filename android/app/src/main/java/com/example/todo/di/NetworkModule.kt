package com.example.todo.di

import com.example.todo.BuildConfig
import com.example.todo.data.local.SessionCookieStore
import com.example.todo.data.remote.api.AuthApiService
import com.example.todo.data.remote.api.TaskApiService
import com.example.todo.data.remote.interceptor.CsrfHeaderInterceptor
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
 * `auth-core` xác thực qua session cookie (KHÔNG phải Bearer JWT) -> `cookieJar(sessionCookieStore)`
 * tự động gắn/lưu `Cookie`/`Set-Cookie` cho mọi request, không cần interceptor gắn header thủ công
 * như `AuthHeaderInterceptor` lịch sử. [CsrfHeaderInterceptor] gắn thêm `x-csrf-token` cho request
 * thay đổi dữ liệu.
 *
 * Thứ tự interceptor: loggingInterceptor TRƯỚC csrfHeaderInterceptor để log ghi lại request ở
 * dạng CHƯA có header CSRF (không ảnh hưởng bảo mật như JWT nhưng giữ nguyên convention cũ).
 *
 * BASE_URL đọc theo build variant qua BuildConfig (xem app/build.gradle.kts) — chỉ là
 * `scheme://host:port/`, mỗi endpoint Retrofit tự thêm tiền tố `api/v1/` (xem AuthApiService/
 * TaskApiService) khớp `@Controller('api/v1/...')` của auth-core.
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
        sessionCookieStore: SessionCookieStore,
        csrfHeaderInterceptor: CsrfHeaderInterceptor,
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
            .cookieJar(sessionCookieStore)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(csrfHeaderInterceptor)
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
