package com.example.todo.presentation.navigation

import app.cash.turbine.test
import com.example.todo.core.session.SessionEvent
import com.example.todo.core.session.SessionEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho [SessionObserverViewModel] — xác nhận `sessionEvents` chỉ là một tấm gương
 * (mirror) trực tiếp của [SessionEventBus.events]: mọi [SessionEvent] emit vào bus phải xuất hiện
 * lại đúng thứ tự qua ViewModel, không bị biến đổi/lọc/trễ thêm.
 *
 * LƯU Ý: máy phát triển hiện tại KHÔNG có Android SDK cài sẵn (ghi nhận từ Phase 0), nên
 * `./gradlew :app:testDebugUnitTest` CHƯA được chạy thật để xác nhận pass/fail — code đã được rà
 * soát thủ công kỹ (import, API Turbine/SharedFlow) nhưng CHƯA verify bằng cách chạy thật.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionObserverViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emit SessionExpired vao bus thi sessionEvents phat lai dung su kien`() = runTest(testDispatcher) {
        val bus = SessionEventBus()
        val viewModel = SessionObserverViewModel(bus)

        viewModel.sessionEvents.test {
            bus.emit(SessionEvent.SessionExpired)

            val received = awaitItem()
            assertEquals(SessionEvent.SessionExpired, received)
        }
    }

    @Test
    fun `nhieu lan emit lien tiep thi sessionEvents phat lai dung thu tu va so luong`() = runTest(testDispatcher) {
        val bus = SessionEventBus()
        val viewModel = SessionObserverViewModel(bus)

        viewModel.sessionEvents.test {
            bus.emit(SessionEvent.SessionExpired)
            assertEquals(SessionEvent.SessionExpired, awaitItem())

            bus.emit(SessionEvent.SessionExpired)
            assertEquals(SessionEvent.SessionExpired, awaitItem())
        }
    }

    @Test
    fun `subscriber moi sau khi event da emit thi khong nhan lai event cu (replay = 0)`() = runTest(testDispatcher) {
        val bus = SessionEventBus()

        // Emit TRƯỚC khi có observer nào collect -- mô phỏng đúng tình huống thực tế: 401 xảy ra,
        // SessionExpiredInterceptor emit, nhưng do extraBufferCapacity = 1 nên không bị mất event
        // (vẫn nằm trong buffer). Tuy nhiên replay = 0 nghĩa là một subscriber MỚI bắt đầu collect
        // SAU đó qua ViewModel mới sẽ không thấy lại event đã emit trước khi nó subscribe.
        bus.emit(SessionEvent.SessionExpired)

        val lateViewModel = SessionObserverViewModel(bus)
        lateViewModel.sessionEvents.test {
            expectNoEvents()
        }
    }
}
