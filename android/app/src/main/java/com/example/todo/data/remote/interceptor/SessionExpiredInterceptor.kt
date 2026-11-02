package com.example.todo.data.remote.interceptor

import com.example.todo.core.session.SessionEvent
import com.example.todo.core.session.SessionEventBus
import com.example.todo.data.local.SessionCookieStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Phát hiện 401 toàn cục -> xóa session cookie cục bộ + emit [SessionEvent.SessionExpired] —
 * theo LLD mục 3.7. Phần ĐIỀU HƯỚNG thật (collect SessionEventBus tại NavHost gốc,
 * SessionObserverViewModel, popUpTo toàn bộ backstack) ở NavGraph.
 *
 * `intercept()` không phải suspend fun -> mọi lời gọi suspend (`clear()`, `emit()`) phải launch
 * trong [appScope] riêng, không thể gọi trực tiếp.
 */
class SessionExpiredInterceptor @Inject constructor(
    private val sessionCookieStore: SessionCookieStore,
    private val sessionEventBus: SessionEventBus,
    private val appScope: CoroutineScope
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            appScope.launch {
                sessionCookieStore.clear()
                sessionEventBus.emit(SessionEvent.SessionExpired)
            }
        }
        return response
    }
}
