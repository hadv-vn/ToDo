package com.example.todo.data.remote.interceptor

import com.example.todo.core.session.SessionEvent
import com.example.todo.core.session.SessionEventBus
import com.example.todo.data.local.TokenLocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Phát hiện 401 toàn cục -> xóa token cục bộ + emit [SessionEvent.SessionExpired] — theo LLD mục
 * 3.7. Đây là bản dò-401-và-dọn-state cơ bản; phần ĐIỀU HƯỚNG thật (collect SessionEventBus tại
 * NavHost gốc, SessionObserverViewModel, popUpTo toàn bộ backstack) thuộc Phase 4 (hardening),
 * KHÔNG implement ở Phase 1 theo đúng phạm vi được giao.
 *
 * `intercept()` không phải suspend fun -> mọi lời gọi suspend (`clearToken()`, `emit()`) phải
 * launch trong [appScope] riêng, không thể gọi trực tiếp.
 */
class SessionExpiredInterceptor @Inject constructor(
    private val tokenLocalDataSource: TokenLocalDataSource,
    private val sessionEventBus: SessionEventBus,
    private val appScope: CoroutineScope
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            appScope.launch {
                tokenLocalDataSource.clearToken()
                sessionEventBus.emit(SessionEvent.SessionExpired)
            }
        }
        return response
    }
}
