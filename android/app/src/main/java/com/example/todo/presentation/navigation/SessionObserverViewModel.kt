package com.example.todo.presentation.navigation

import androidx.lifecycle.ViewModel
import com.example.todo.core.session.SessionEvent
import com.example.todo.core.session.SessionEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * Theo LLD mục 4.4(c) — cầu nối giữa [SessionEventBus] (Hilt Singleton, sống cùng Application) và
 * NavGraph (Composable, cần một ViewModel để `collect` SharedFlow đúng lifecycle-aware qua
 * `hiltViewModel()`).
 *
 * Chỉ expose lại `events` dưới tên `sessionEvents` — không giữ thêm state nào khác. Được tạo ở
 * phạm vi gần root nhất có thể (NavGraph, ngoài mọi `composable()` destination) để sống suốt đời
 * Activity, không bị hủy theo backstack entry nào — đảm bảo KHÔNG bỏ lỡ sự kiện SessionExpired dù
 * nó xảy ra ở route nào.
 */
@HiltViewModel
class SessionObserverViewModel @Inject constructor(
    sessionEventBus: SessionEventBus
) : ViewModel() {
    val sessionEvents: SharedFlow<SessionEvent> = sessionEventBus.events
}
