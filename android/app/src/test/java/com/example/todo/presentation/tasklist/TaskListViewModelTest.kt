package com.example.todo.presentation.tasklist

import app.cash.turbine.test
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskFilter
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.usecase.auth.LogoutUseCase
import com.example.todo.domain.usecase.task.DeleteTaskUseCase
import com.example.todo.domain.usecase.task.GetTasksUseCase
import com.example.todo.domain.usecase.task.ToggleTaskStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit test cho [TaskListViewModel] — MockK cho UseCase, Turbine cho StateFlow. Theo LLD mục 4.4(a)
 * (double-tap protection + rollback) và mục 8 (gợi ý test).
 *
 * LƯU Ý QUAN TRỌNG: máy phát triển hiện tại KHÔNG có Android SDK cài sẵn (ghi nhận từ Phase 0/1),
 * nên `./gradlew :app:testDebugUnitTest` CHƯA được chạy thật để xác nhận pass/fail. Code test này
 * đã được rà soát thủ công kỹ (import, kiểu dữ liệu, thứ tự state emit dưới UnconfinedTestDispatcher)
 * nhưng CHƯA verify bằng cách chạy thật.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getTasksUseCase: GetTasksUseCase = mockk()
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase = mockk()
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk()

    private val taskOpen = Task(
        id = "task-1",
        title = "Mua sữa",
        description = null,
        status = TaskStatus.OPEN,
        createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
        updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TaskListViewModel {
        return TaskListViewModel(getTasksUseCase, toggleTaskStatusUseCase, deleteTaskUseCase, logoutUseCase)
    }

    @Test
    fun `onToggleTask goi lan 2 khi request dau chua xong thi chi goi UseCase 1 lan`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        // toggleTaskStatusUseCase CHƯA trả kết quả ngay (CompletableDeferred) để mô phỏng request
        // đầu tiên chưa hoàn tất khi tap lần 2 xảy ra ngay sau đó.
        val pending = CompletableDeferred<ApiResult<Task>>()
        coEvery { toggleTaskStatusUseCase(any(), any()) } coAnswers { pending.await() }

        val viewModel = createViewModel()

        viewModel.uiState.test {
            // init{} -> loadTasks() đã chạy xong đồng bộ dưới UnconfinedTestDispatcher (không có
            // suspension thật nào) trước khi .test{} bắt đầu collect -> StateFlow chỉ còn replay
            // giá trị ĐÃ settled (tasks=[taskOpen], isLoading=false) làm item đầu tiên.
            val afterLoad = awaitItem()
            assertEquals(TaskStatus.OPEN, afterLoad.tasks.first().status)

            viewModel.onToggleTask("task-1") // tap 1 -> optimistic update, coroutine suspend ở pending.await()
            val afterFirstTap = awaitItem()
            assertEquals(TaskStatus.DONE, afterFirstTap.tasks.first().status)
            assertTrue(afterFirstTap.tasks.first().isTogglePending)

            viewModel.onToggleTask("task-1") // tap 2 ngay sau đó -> bị bỏ qua vì isTogglePending=true

            pending.complete(ApiResult.Success(taskOpen.copy(status = TaskStatus.DONE)))
            val afterSuccess = awaitItem()
            assertFalse(afterSuccess.tasks.first().isTogglePending)
        }

        coVerify(exactly = 1) { toggleTaskStatusUseCase("task-1", TaskStatus.OPEN) }
    }

    @Test
    fun `onToggleTask loi mang thi rollback ve trang thai cu`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        coEvery { toggleTaskStatusUseCase("task-1", TaskStatus.OPEN) } returns ApiResult.NetworkError

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // sau init -> load thành công (OPEN)

            viewModel.onToggleTask("task-1")
            val optimistic = awaitItem() // optimistic update: DONE, isTogglePending=true
            assertEquals(TaskStatus.DONE, optimistic.tasks.first().status)

            val afterRollback = awaitItem()
            assertEquals(TaskStatus.OPEN, afterRollback.tasks.first().status)
            assertFalse(afterRollback.tasks.first().isTogglePending)
            assertEquals(true, afterRollback.error?.isNetworkError)
        }

        coVerify(exactly = 1) { toggleTaskStatusUseCase("task-1", TaskStatus.OPEN) }
    }

    @Test
    fun `onToggleTask 404 thi loai task khoi danh sach khong rollback status`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        coEvery { toggleTaskStatusUseCase("task-1", TaskStatus.OPEN) } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // sau init -> load thành công (OPEN)

            viewModel.onToggleTask("task-1")
            val optimistic = awaitItem() // optimistic update: DONE, isTogglePending=true
            assertEquals(TaskStatus.DONE, optimistic.tasks.first().status)

            val afterNotFound = awaitItem()
            assertTrue(afterNotFound.tasks.isEmpty()) // loại hẳn khỏi state, KHÔNG rollback status
        }
    }

    // --- computeHasAnyTaskOverall/computeEmptyState (LLD mục 4.4f, NGUYÊN VĂN pseudocode) ---
    // 4 tổ hợp: ALL rỗng, ALL có dữ liệu, Open rỗng (ALL trước đó có task), Open rỗng (ALL trước
    // đó cũng rỗng) — test gián tiếp qua loadTasks()/onFilterSelected() vì 2 hàm này là `private`.

    @Test
    fun `ALL rong thi emptyStateType la NO_TASKS_AT_ALL va hasAnyTaskOverall la false`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(emptyList())

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val afterLoad = awaitItem() // init -> loadTasks(ALL) rỗng
            assertTrue(afterLoad.tasks.isEmpty())
            assertEquals(EmptyStateType.NO_TASKS_AT_ALL, afterLoad.emptyStateType)
            assertFalse(afterLoad.hasAnyTaskOverall)
        }
    }

    @Test
    fun `ALL co du lieu thi emptyStateType null va hasAnyTaskOverall la true`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(listOf(taskOpen))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val afterLoad = awaitItem() // init -> loadTasks(ALL) có 1 task
            assertFalse(afterLoad.tasks.isEmpty())
            assertEquals(null, afterLoad.emptyStateType)
            assertTrue(afterLoad.hasAnyTaskOverall)
        }
    }

    @Test
    fun `OPEN rong nhung ALL truoc do co task thi NO_TASKS_MATCH_FILTER`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(listOf(taskOpen))
        coEvery { getTasksUseCase(TaskFilter.OPEN) } returns ApiResult.Success(emptyList())

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // init -> loadTasks(ALL) có 1 task -> hasAnyTaskOverall=true

            viewModel.onFilterSelected(TaskFilter.OPEN)
            // tasks (của ALL) chưa rỗng -> onFilterSelected gọi loadTasks(showLoading=false) -> chỉ 1
            // emission nữa từ chính onFilterSelected (đổi filter) rồi tới emission kết quả load OPEN.
            val afterFilterChanged = awaitItem()
            assertEquals(TaskFilter.OPEN, afterFilterChanged.filter)

            val afterLoadOpen = awaitItem()
            assertTrue(afterLoadOpen.tasks.isEmpty())
            assertEquals(EmptyStateType.NO_TASKS_MATCH_FILTER, afterLoadOpen.emptyStateType)
            assertTrue(afterLoadOpen.hasAnyTaskOverall) // không hạ cờ nếu đã từng biết có task
        }
    }

    @Test
    fun `OPEN rong va ALL truoc do cung rong thi NO_TASKS_AT_ALL`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(emptyList())
        coEvery { getTasksUseCase(TaskFilter.OPEN) } returns ApiResult.Success(emptyList())

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val afterInit = awaitItem() // init -> loadTasks(ALL) rỗng -> hasAnyTaskOverall=false
            assertFalse(afterInit.hasAnyTaskOverall)

            viewModel.onFilterSelected(TaskFilter.OPEN)
            // tasks (của ALL) đang rỗng -> onFilterSelected gọi loadTasks(showLoading=true) -> 3
            // emission: (1) đổi filter, (2) isLoading=true, (3) kết quả load OPEN.
            val afterFilterChanged = awaitItem()
            assertEquals(TaskFilter.OPEN, afterFilterChanged.filter)

            val duringLoad = awaitItem()
            assertTrue(duringLoad.isLoading)

            val afterLoadOpen = awaitItem()
            assertFalse(afterLoadOpen.isLoading)
            assertTrue(afterLoadOpen.tasks.isEmpty())
            assertEquals(EmptyStateType.NO_TASKS_AT_ALL, afterLoadOpen.emptyStateType)
            assertFalse(afterLoadOpen.hasAnyTaskOverall) // không có gì để biết tổng thể có task
        }
    }

    // --- onPullToRefresh (LLD mục 4.4e, NGUYÊN VĂN pseudocode) ---

    @Test
    fun `onPullToRefresh loi thi khong xoa tasks cu, chi tat isRefreshing va bao loi`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.ALL) } returnsMany listOf(
            ApiResult.Success(listOf(taskOpen)), // lần load đầu (init)
            ApiResult.NetworkError // lần gọi lại do onPullToRefresh
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val afterInit = awaitItem()
            assertEquals(1, afterInit.tasks.size)

            viewModel.onPullToRefresh()
            val duringRefresh = awaitItem()
            assertTrue(duringRefresh.isRefreshing)
            assertEquals(1, duringRefresh.tasks.size) // KHÔNG xóa tasks cũ khi bắt đầu refresh

            val afterRefreshError = awaitItem()
            assertFalse(afterRefreshError.isRefreshing)
            assertEquals(1, afterRefreshError.tasks.size) // KHÔNG xóa tasks cũ khi lỗi
            assertEquals("task-1", afterRefreshError.tasks.first().id)
            assertEquals(true, afterRefreshError.error?.isNetworkError)
        }
    }
}
