package com.example.todo.presentation.tasklist

import app.cash.turbine.test
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskFilter
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.usecase.auth.LogoutUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.domain.usecase.preferences.GetInsertPositionPreferenceUseCase
import com.example.todo.domain.usecase.preferences.SetInsertPositionPreferenceUseCase
import com.example.todo.domain.usecase.task.DeleteTaskUseCase
import com.example.todo.domain.usecase.task.GetTasksUseCase
import com.example.todo.domain.usecase.task.ReorderTaskUseCase
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
    private val reorderTaskUseCase: ReorderTaskUseCase = mockk()
    private val getInsertPositionPreferenceUseCase: GetInsertPositionPreferenceUseCase = mockk()
    private val setInsertPositionPreferenceUseCase: SetInsertPositionPreferenceUseCase = mockk()
    private val getCategoriesUseCase: GetCategoriesUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk()

    private val taskOpen = Task(
        id = "task-1",
        title = "Mua sữa",
        description = null,
        status = TaskStatus.OPEN,
        createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
        updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
    )
    private val taskDone = Task(
        id = "task-2",
        title = "Don nha",
        description = null,
        status = TaskStatus.DONE,
        createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
        updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // init{} của TaskListViewModel luôn gọi use case này — mặc định TOP cho mọi test không
        // liên quan tới FR-26, tránh MockKException "no answer found".
        coEvery { getInsertPositionPreferenceUseCase() } returns InsertPosition.TOP
        // loadTasks()/onPullToRefresh()/init{} đều gọi loadCategories() — mặc định rỗng cho mọi
        // test không liên quan tới FR-24.
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TaskListViewModel {
        return TaskListViewModel(
            getTasksUseCase,
            toggleTaskStatusUseCase,
            deleteTaskUseCase,
            reorderTaskUseCase,
            getInsertPositionPreferenceUseCase,
            setInsertPositionPreferenceUseCase,
            getCategoriesUseCase,
            logoutUseCase
        )
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

    @Test
    fun `onPullToRefresh thanh cong thi cap nhat du lieu va giu nguyen filter dang ap dung`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.OPEN) } returnsMany listOf(
            ApiResult.Success(listOf(taskOpen)), // lần gọi khi onFilterSelected(OPEN)
            ApiResult.Success(listOf(taskOpen, taskDone)) // lần gọi lại do onPullToRefresh, có thêm task mới
        )
        coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(listOf(taskOpen))

        val viewModel = createViewModel()

        // Không dùng Turbine đếm từng emission ở đây — mockk `returns`/`returnsMany` không có suspension
        // thật nào dưới UnconfinedTestDispatcher, nên toàn bộ chuỗi update bên trong mỗi lệnh gọi đã
        // chạy xong đồng bộ ngay khi hàm gọi nó return; `.value` đọc trực tiếp state đã settled cuối
        // cùng, tránh phải đếm chính xác số lần StateFlow phát ra (dễ sai, xem các test onDeleteTaskConfirmed).
        viewModel.onFilterSelected(TaskFilter.OPEN)
        assertEquals(TaskFilter.OPEN, viewModel.uiState.value.filter)
        assertEquals(1, viewModel.uiState.value.tasks.size)

        viewModel.onPullToRefresh()
        val afterRefresh = viewModel.uiState.value
        assertEquals(TaskFilter.OPEN, afterRefresh.filter) // vẫn giữ filter OPEN
        assertEquals(2, afterRefresh.tasks.size)
        assertFalse(afterRefresh.isRefreshing)
    }

    // --- onFilterSelected (TC-FILTER-01/02/03) — chọn chip lọc đúng nội dung hiển thị ---

    @Test
    fun `onFilterSelected OPEN thi chi con task dang mo trong danh sach`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(listOf(taskOpen, taskDone))
        coEvery { getTasksUseCase(TaskFilter.OPEN) } returns ApiResult.Success(listOf(taskOpen))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // init -> ALL: [taskOpen, taskDone]

            viewModel.onFilterSelected(TaskFilter.OPEN)
            val afterFilterChanged = awaitItem() // đổi filter, giữ danh sách cũ (showLoading=false vì tasks không rỗng)
            assertEquals(TaskFilter.OPEN, afterFilterChanged.filter)

            val afterLoadOpen = awaitItem()
            assertEquals(1, afterLoadOpen.tasks.size)
            assertEquals("task-1", afterLoadOpen.tasks.first().id)
        }
    }

    @Test
    fun `onFilterSelected DONE thi chi con task da xong trong danh sach`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(listOf(taskOpen, taskDone))
        coEvery { getTasksUseCase(TaskFilter.DONE) } returns ApiResult.Success(listOf(taskDone))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // init -> ALL: [taskOpen, taskDone]

            viewModel.onFilterSelected(TaskFilter.DONE)
            awaitItem() // đổi filter

            val afterLoadDone = awaitItem()
            assertEquals(1, afterLoadDone.tasks.size)
            assertEquals("task-2", afterLoadDone.tasks.first().id)
        }
    }

    @Test
    fun `onFilterSelected ve ALL sau khi dang loc thi hien lai toan bo task`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(listOf(taskOpen, taskDone))
        coEvery { getTasksUseCase(TaskFilter.OPEN) } returns ApiResult.Success(listOf(taskOpen))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // init -> ALL: [taskOpen, taskDone]

            viewModel.onFilterSelected(TaskFilter.OPEN)
            awaitItem() // đổi filter
            awaitItem() // load OPEN: [taskOpen]

            viewModel.onFilterSelected(TaskFilter.ALL)
            awaitItem() // đổi filter
            val afterBackToAll = awaitItem()
            assertEquals(2, afterBackToAll.tasks.size)
        }
    }

    // --- onDeleteTaskConfirmed (TC-DEL-03/04/05) ---

    // Không dùng Turbine đếm từng emission cho 4 test onDeleteTaskConfirmed dưới đây — mỗi nhánh kết
    // quả cập nhật `_uiState` qua 1-2 lệnh `.update{}` liên tiếp KHÔNG có suspension thật nào chen giữa
    // (mockk `returns` trả ngay, không phải CompletableDeferred), nên dưới UnconfinedTestDispatcher
    // toàn bộ chuỗi đã chạy xong đồng bộ ngay khi `onDeleteTaskConfirmed()` return — đọc thẳng `.value`
    // đơn giản và đúng hơn là đếm chính xác số lần StateFlow phát ra.

    @Test
    fun `onDeleteTaskConfirmed thanh cong thi loai task khoi danh sach`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        coEvery { deleteTaskUseCase("task-1") } returns ApiResult.Success(Unit)

        val viewModel = createViewModel()

        viewModel.onDeleteTaskConfirmed("task-1")

        val afterDelete = viewModel.uiState.value
        assertTrue(afterDelete.tasks.isEmpty())
        assertTrue("task-1" !in afterDelete.pendingDeleteTaskIds)
        coVerify(exactly = 1) { deleteTaskUseCase("task-1") }
    }

    @Test
    fun `onDeleteTaskConfirmed 404 thi loai task va bao da bi xoa truoc do`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        coEvery { deleteTaskUseCase("task-1") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")

        val viewModel = createViewModel()

        viewModel.onDeleteTaskConfirmed("task-1")

        val afterNotFound = viewModel.uiState.value
        assertTrue(afterNotFound.tasks.isEmpty())
        assertEquals("Task này đã được xóa trước đó", afterNotFound.error?.message)
    }

    @Test
    fun `onDeleteTaskConfirmed 403 thi giu task va bao khong co quyen`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        coEvery { deleteTaskUseCase("task-1") } returns
            ApiResult.Error(ApiErrorCode.FORBIDDEN, "Forbidden")

        val viewModel = createViewModel()

        viewModel.onDeleteTaskConfirmed("task-1")

        val afterForbidden = viewModel.uiState.value
        assertEquals(1, afterForbidden.tasks.size) // KHÔNG loại khỏi danh sách
        assertEquals("Bạn không có quyền thực hiện thao tác này", afterForbidden.error?.message)
        assertTrue("task-1" !in afterForbidden.pendingDeleteTaskIds)
    }

    @Test
    fun `onDeleteTaskConfirmed mat mang thi giu task trong danh sach va bao loi mang`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        coEvery { deleteTaskUseCase("task-1") } returns ApiResult.NetworkError

        val viewModel = createViewModel()

        viewModel.onDeleteTaskConfirmed("task-1")

        val afterNetworkError = viewModel.uiState.value
        assertEquals(1, afterNetworkError.tasks.size) // task vẫn còn
        assertEquals(true, afterNetworkError.error?.isNetworkError)
    }

    @Test
    fun `onDeleteTaskConfirmed goi lan 2 khi lan dau chua xong thi chi goi UseCase 1 lan`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        val pending = CompletableDeferred<ApiResult<Unit>>()
        coEvery { deleteTaskUseCase("task-1") } coAnswers { pending.await() }

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // init -> [taskOpen]

            viewModel.onDeleteTaskConfirmed("task-1") // lần 1 -> pendingDeleteTaskIds += task-1, suspend ở pending.await()
            val afterFirstConfirm = awaitItem()
            assertTrue("task-1" in afterFirstConfirm.pendingDeleteTaskIds)

            viewModel.onDeleteTaskConfirmed("task-1") // lần 2 ngay sau đó -> bị bỏ qua (đã nằm trong pendingDeleteTaskIds)

            pending.complete(ApiResult.Success(Unit))
            val afterSuccess = awaitItem()
            assertTrue(afterSuccess.tasks.isEmpty())
        }

        coVerify(exactly = 1) { deleteTaskUseCase("task-1") }
    }

    // --- Bug đã sửa: xóa/loại task cuối cùng khớp filter hiện tại phải tính lại emptyStateType,
    // không được để màn hình trắng (xem TaskListViewModel.stateAfterRemovingTask) ---

    @Test
    fun `xoa thanh cong task cuoi cung khop filter thi emptyStateType duoc tinh lai dung`() =
        runTest(testDispatcher) {
            coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(listOf(taskOpen, taskDone))
            coEvery { getTasksUseCase(TaskFilter.DONE) } returns ApiResult.Success(listOf(taskDone))
            coEvery { deleteTaskUseCase("task-2") } returns ApiResult.Success(Unit)

            val viewModel = createViewModel()
            viewModel.onFilterSelected(TaskFilter.DONE)
            assertEquals(1, viewModel.uiState.value.tasks.size)

            viewModel.onDeleteTaskConfirmed("task-2")

            val after = viewModel.uiState.value
            assertTrue(after.tasks.isEmpty())
            // Trước khi sửa bug: emptyStateType vẫn là null (giá trị cũ từ lúc còn 1 task) -> UI
            // không khớp nhánh list lẫn empty-state, màn hình trắng trơn.
            assertEquals(EmptyStateType.NO_TASKS_MATCH_FILTER, after.emptyStateType)
        }

    @Test
    fun `toggle 404 loai task cuoi cung khop filter thi emptyStateType duoc tinh lai dung`() =
        runTest(testDispatcher) {
            coEvery { getTasksUseCase(TaskFilter.ALL) } returns ApiResult.Success(listOf(taskOpen))
            coEvery { getTasksUseCase(TaskFilter.OPEN) } returns ApiResult.Success(listOf(taskOpen))
            coEvery { toggleTaskStatusUseCase("task-1", TaskStatus.OPEN) } returns
                ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")

            val viewModel = createViewModel()
            viewModel.onFilterSelected(TaskFilter.OPEN)
            assertEquals(1, viewModel.uiState.value.tasks.size)

            viewModel.onToggleTask("task-1")

            val after = viewModel.uiState.value
            assertTrue(after.tasks.isEmpty())
            assertEquals(EmptyStateType.NO_TASKS_MATCH_FILTER, after.emptyStateType)
        }

    // --- onToggleTask 403 (ownership, TC-TOGGLE adjacent/TC-OWN-02) ---

    @Test
    fun `onToggleTask 403 thi rollback ve trang thai cu va bao khong co quyen`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))
        coEvery { toggleTaskStatusUseCase("task-1", TaskStatus.OPEN) } returns
            ApiResult.Error(ApiErrorCode.FORBIDDEN, "Forbidden")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // sau init -> load thành công (OPEN)

            viewModel.onToggleTask("task-1")
            val optimistic = awaitItem() // optimistic update: DONE
            assertEquals(TaskStatus.DONE, optimistic.tasks.first().status)

            val afterForbidden = awaitItem()
            assertEquals(TaskStatus.OPEN, afterForbidden.tasks.first().status) // rollback
            assertEquals("Bạn không có quyền thực hiện thao tác này", afterForbidden.error?.message)
        }
    }

    // --- kéo-thả (FR-25, docs/Task_Reorder_ToDo.md): onTaskDragStarted/Stepped/Ended ---

    @Test
    fun `onTaskDragStepped doi vi tri cuc bo ngay, KHONG goi ReorderTaskUseCase`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen, taskDone))

        val viewModel = createViewModel()
        viewModel.onTaskDragStarted()
        // ban đầu: [task-1, task-2] -> kéo task-2 (index 1) lên đầu (index 0)
        viewModel.onTaskDragStepped(fromIndex = 1, toIndex = 0)

        assertEquals(listOf("task-2", "task-1"), viewModel.uiState.value.tasks.map { it.id })
        coVerify(exactly = 0) { reorderTaskUseCase(any(), any(), any()) }
    }

    @Test
    fun `onTaskDragEnded goi ReorderTaskUseCase 1 lan voi dung hang xom cuoi cung`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen, taskDone))
        val pending = CompletableDeferred<ApiResult<Task>>()
        coEvery { reorderTaskUseCase(any(), any(), any()) } coAnswers { pending.await() }

        val viewModel = createViewModel()
        viewModel.onTaskDragStarted()
        viewModel.onTaskDragStepped(fromIndex = 1, toIndex = 0) // kéo qua nhiều bước (ở đây 1 bước)
        viewModel.onTaskDragEnded("task-2")

        coVerify(exactly = 1) { reorderTaskUseCase("task-2", null, "task-1") }
        pending.complete(ApiResult.Success(taskDone))
    }

    @Test
    fun `onTaskDragEnded loi thi rollback ve thu tu LUC BAT DAU keo va bao loi`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen, taskDone))
        coEvery { reorderTaskUseCase(any(), any(), any()) } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // sau init load thành công

            viewModel.onTaskDragStarted()
            viewModel.onTaskDragStepped(fromIndex = 1, toIndex = 0)
            val optimistic = awaitItem()
            assertEquals(listOf("task-2", "task-1"), optimistic.tasks.map { it.id })

            viewModel.onTaskDragEnded("task-2")
            val afterError = awaitItem()
            assertEquals(listOf("task-1", "task-2"), afterError.tasks.map { it.id }) // rollback
            assertEquals("Task không tồn tại", afterError.error?.message)
        }
    }

    @Test
    fun `onTaskDragEnded khong doi vi tri thi khong goi server`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen, taskDone))

        val viewModel = createViewModel()
        viewModel.onTaskDragStarted() // long-press nhưng không kéo đi đâu cả
        viewModel.onTaskDragEnded("task-1")

        coVerify(exactly = 0) { reorderTaskUseCase(any(), any(), any()) }
    }

    @Test
    fun `onTaskDragStepped voi fromIndex bang toIndex thi khong lam gi`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen, taskDone))

        val viewModel = createViewModel()
        viewModel.onTaskDragStarted()
        viewModel.onTaskDragStepped(fromIndex = 0, toIndex = 0)

        assertEquals(listOf("task-1", "task-2"), viewModel.uiState.value.tasks.map { it.id })
    }

    // --- FR-26 — cài đặt vị trí thêm task mới ---

    @Test
    fun `init tai dung insertPositionPreference tu use case`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(emptyList())
        coEvery { getInsertPositionPreferenceUseCase() } returns InsertPosition.BOTTOM

        val viewModel = createViewModel()

        assertEquals(InsertPosition.BOTTOM, viewModel.uiState.value.insertPositionPreference)
    }

    @Test
    fun `onInsertPositionSelected cap nhat state ngay va luu qua use case`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(emptyList())
        coEvery { setInsertPositionPreferenceUseCase(InsertPosition.BOTTOM) } returns Unit

        val viewModel = createViewModel()
        viewModel.onInsertPositionSettingsClicked()
        assertTrue(viewModel.uiState.value.showInsertPositionDialog)

        viewModel.onInsertPositionSelected(InsertPosition.BOTTOM)

        assertEquals(InsertPosition.BOTTOM, viewModel.uiState.value.insertPositionPreference)
        assertFalse(viewModel.uiState.value.showInsertPositionDialog)
        coVerify(exactly = 1) { setInsertPositionPreferenceUseCase(InsertPosition.BOTTOM) }
    }

    // --- isOverdue (FR-21, docs/Task_DueDate_ToDo.md) ---

    @Test
    fun `task OPEN qua han thi isOverdue true`() = runTest(testDispatcher) {
        val pastDueDate = java.time.LocalDate.now().minusDays(1)
        coEvery { getTasksUseCase(any()) } returns
            ApiResult.Success(listOf(taskOpen.copy(dueDate = pastDueDate)))

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.tasks.first().isOverdue)
    }

    @Test
    fun `task DONE qua han thi isOverdue false (da xong khong con la van de qua han)`() = runTest(testDispatcher) {
        val pastDueDate = java.time.LocalDate.now().minusDays(1)
        coEvery { getTasksUseCase(any()) } returns
            ApiResult.Success(listOf(taskDone.copy(dueDate = pastDueDate)))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.tasks.first().isOverdue)
    }

    @Test
    fun `task OPEN chua toi han thi isOverdue false`() = runTest(testDispatcher) {
        val futureDueDate = java.time.LocalDate.now().plusDays(1)
        coEvery { getTasksUseCase(any()) } returns
            ApiResult.Success(listOf(taskOpen.copy(dueDate = futureDueDate)))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.tasks.first().isOverdue)
    }

    @Test
    fun `task khong co dueDate thi isOverdue false`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.tasks.first().isOverdue)
    }

    // --- priority (FR-19, docs/Task_Priority_ToDo.md) ---

    @Test
    fun `mapper gan dung priority vao TaskUiModel`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns
            ApiResult.Success(listOf(taskOpen.copy(priority = com.example.todo.domain.model.TaskPriority.HIGH)))

        val viewModel = createViewModel()

        assertEquals(com.example.todo.domain.model.TaskPriority.HIGH, viewModel.uiState.value.tasks.first().priority)
    }

    @Test
    fun `task khong chi dinh priority thi mac dinh MEDIUM`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase(any()) } returns ApiResult.Success(listOf(taskOpen))

        val viewModel = createViewModel()

        assertEquals(com.example.todo.domain.model.TaskPriority.MEDIUM, viewModel.uiState.value.tasks.first().priority)
    }
}
