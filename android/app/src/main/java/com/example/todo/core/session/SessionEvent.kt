package com.example.todo.core.session

/**
 * Sự kiện phát ra toàn app qua [SessionEventBus] — theo LLD mục 3.7.
 */
sealed class SessionEvent {
    data object SessionExpired : SessionEvent()
}
