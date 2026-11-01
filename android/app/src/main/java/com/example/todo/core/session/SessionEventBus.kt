package com.example.todo.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kênh phát sự kiện 401 toàn cục — theo LLD mục 3.7.
 *
 * `replay = 0` để KHÔNG phát lại sự kiện cũ cho observer mới subscribe sau (tránh re-trigger
 * logout khi quay lại Login lần sau); `extraBufferCapacity = 1` để không mất event nếu emit xảy
 * ra đúng lúc observer tạm gián đoạn collect (ví dụ do recomposition).
 *
 * `@Inject constructor` + `@Singleton`: Hilt tự tạo instance duy nhất toàn app, không cần
 * @Provides riêng trong module.
 */
@Singleton
class SessionEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<SessionEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    suspend fun emit(event: SessionEvent) {
        _events.emit(event)
    }
}
