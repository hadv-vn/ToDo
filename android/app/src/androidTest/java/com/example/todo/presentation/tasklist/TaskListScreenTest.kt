package com.example.todo.presentation.tasklist

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.model.Category
import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.repository.AuthRepository
import com.example.todo.domain.repository.CategoryRepository
import com.example.todo.domain.repository.PreferencesRepository
import com.example.todo.domain.repository.TaskRepository
import com.example.todo.domain.usecase.auth.LogoutUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.domain.usecase.preferences.GetInsertPositionPreferenceUseCase
import com.example.todo.domain.usecase.preferences.SetInsertPositionPreferenceUseCase
import com.example.todo.domain.usecase.task.DeleteTaskUseCase
import com.example.todo.domain.usecase.task.GetTasksUseCase
import com.example.todo.domain.usecase.task.ReorderTaskUseCase
import com.example.todo.domain.usecase.task.ToggleTaskStatusUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Compose UI test cho luồng danh sách Task (LLD mục 6.4/8) — checkbox phản ánh đúng trạng thái
 * done/open, và swipe trên 1 row mở ra dialog xác nhận xóa.
 *
 * Không dùng MockK (androidTestImplementation chưa có `mockk-android`) — dùng
 * [FakeTaskRepositoryWithFixedList]/[FakeTaskRepositoryFilterAware]/[FakeAuthRepositoryLogoutOnly]
 * viết tay implement interface.
 *
 * Đã chạy thật qua `./gradlew :app:connectedDebugAndroidTest` trên emulator (2026-11-14) — toàn bộ
 * test trong file này PASS, bao gồm swipe-to-delete (ngưỡng kéo `endX` vượt xa mép trái node khớp
 * được, xem [swipeTrenMotRow_thiHienDialogXacNhanXoa]).
 */
class TaskListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val openTask = Task(
        id = "task-open",
        title = "Task Dang Mo",
        description = null,
        status = TaskStatus.OPEN,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
    private val doneTask = Task(
        id = "task-done",
        title = "Task Da Xong",
        description = null,
        status = TaskStatus.DONE,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private class FakeTaskRepositoryWithFixedList(private val tasks: List<Task>) : TaskRepository {
        override suspend fun getTasks(filter: TaskStatus?): ApiResult<List<Task>> = ApiResult.Success(tasks)
        override suspend fun getTaskById(id: String) = throw NotImplementedError()
        override suspend fun createTask(
            title: String,
            description: String?,
            insertPosition: InsertPosition,
            categoryId: String?,
            dueDate: java.time.LocalDate?,
            priority: com.example.todo.domain.model.TaskPriority
        ) = throw NotImplementedError()
        override suspend fun editTask(
            id: String,
            title: String,
            description: String?,
            categoryId: String?,
            dueDate: java.time.LocalDate?,
            priority: com.example.todo.domain.model.TaskPriority
        ) = throw NotImplementedError()
        override suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus): ApiResult<Task> {
            val task = tasks.first { it.id == id }
            return ApiResult.Success(task.copy(status = newStatus))
        }
        override suspend fun reorderTask(id: String, afterTaskId: String?, beforeTaskId: String?) =
            throw NotImplementedError()
        override suspend fun deleteTask(id: String): ApiResult<Unit> = ApiResult.Success(Unit)
    }

    /** Lọc đúng theo `filter` (null = tất cả) — dùng cho test FilterChip (TC-FILTER). */
    private class FakeTaskRepositoryFilterAware(
        private val allTasks: List<Task>,
        private val deleteResult: ApiResult<Unit> = ApiResult.Success(Unit)
    ) : TaskRepository {
        override suspend fun getTasks(filter: TaskStatus?): ApiResult<List<Task>> {
            val filtered = if (filter == null) allTasks else allTasks.filter { it.status == filter }
            return ApiResult.Success(filtered)
        }
        override suspend fun getTaskById(id: String) = throw NotImplementedError()
        override suspend fun createTask(
            title: String,
            description: String?,
            insertPosition: InsertPosition,
            categoryId: String?,
            dueDate: java.time.LocalDate?,
            priority: com.example.todo.domain.model.TaskPriority
        ) = throw NotImplementedError()
        override suspend fun editTask(
            id: String,
            title: String,
            description: String?,
            categoryId: String?,
            dueDate: java.time.LocalDate?,
            priority: com.example.todo.domain.model.TaskPriority
        ) = throw NotImplementedError()
        override suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus): ApiResult<Task> {
            val task = allTasks.first { it.id == id }
            return ApiResult.Success(task.copy(status = newStatus))
        }
        override suspend fun reorderTask(id: String, afterTaskId: String?, beforeTaskId: String?) =
            throw NotImplementedError()
        override suspend fun deleteTask(id: String): ApiResult<Unit> = deleteResult
    }

    private class FakePreferencesRepository : PreferencesRepository {
        // FR-26 — ghi lại lần gọi setInsertPosition gần nhất để test xác nhận đã lưu đúng lựa chọn.
        var lastSetPosition: InsertPosition? = null
        override suspend fun getInsertPosition(): InsertPosition = InsertPosition.TOP
        override suspend fun setInsertPosition(position: InsertPosition) {
            lastSetPosition = position
        }
    }

    private class FakeCategoryRepository(private val categories: List<Category> = emptyList()) : CategoryRepository {
        override suspend fun getCategories(): ApiResult<List<Category>> = ApiResult.Success(categories)
        override suspend fun createCategory(name: String, color: String?, icon: String?) = throw NotImplementedError()
        override suspend fun updateCategory(id: String, name: String, color: String, icon: String) =
            throw NotImplementedError()
        override suspend fun deleteCategory(id: String) = throw NotImplementedError()
    }

    private class FakeAuthRepositoryLogoutOnly : AuthRepository {
        override suspend fun register(email: String, password: String) = throw NotImplementedError()
        override suspend fun login(email: String, password: String) = throw NotImplementedError()
        override suspend fun logout() = Unit
        override suspend fun resendVerification(email: String) = throw NotImplementedError()
        override suspend fun forgotPassword(email: String) = throw NotImplementedError()
        override suspend fun resetPassword(token: String, newPassword: String) = throw NotImplementedError()
        override suspend fun hasActiveSession(): Boolean = false
    }

    private fun createListViewModel(tasks: List<Task>): TaskListViewModel =
        createListViewModel(FakeTaskRepositoryWithFixedList(tasks))

    private fun createListViewModel(
        taskRepository: TaskRepository,
        categoryRepository: CategoryRepository = FakeCategoryRepository(),
        preferencesRepository: PreferencesRepository = FakePreferencesRepository()
    ): TaskListViewModel {
        return TaskListViewModel(
            getTasksUseCase = GetTasksUseCase(taskRepository),
            toggleTaskStatusUseCase = ToggleTaskStatusUseCase(taskRepository),
            deleteTaskUseCase = DeleteTaskUseCase(taskRepository),
            reorderTaskUseCase = ReorderTaskUseCase(taskRepository),
            getInsertPositionPreferenceUseCase = GetInsertPositionPreferenceUseCase(preferencesRepository),
            setInsertPositionPreferenceUseCase = SetInsertPositionPreferenceUseCase(preferencesRepository),
            getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
            logoutUseCase = LogoutUseCase(FakeAuthRepositoryLogoutOnly())
        )
    }

    @Test
    fun checkbox_phanAnhDungTrangThaiOpenVaDone() {
        // init{} của TaskListViewModel gọi loadTasks() đồng bộ xong trước khi setContent (không có
        // dispatcher thật nào trì hoãn vì FakeTaskRepository không suspend thật) -> uiState đã có
        // đủ 2 task ngay khi Composable lần đầu compose.
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(listOf(openTask, doneTask)))
        }

        composeTestRule.onNodeWithText("Task Dang Mo").assertExists()
        composeTestRule.onNodeWithText("Task Da Xong").assertExists()

        // Thứ tự Checkbox trong LazyColumn khớp đúng thứ tự `tasks` truyền vào (openTask trước,
        // doneTask sau) — đây là cách duy nhất phân biệt 2 Checkbox khi UI hiện tại không có
        // testTag riêng cho từng row.
        val checkboxes = composeTestRule.onAllNodes(isToggleable())
        checkboxes[0].assertIsOff() // Task Dang Mo -> OPEN -> chưa check
        checkboxes[1].assertIsOn() // Task Da Xong -> DONE -> đã check
    }

    @Test
    fun swipeTrenMotRow_thiHienDialogXacNhanXoa() {
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(listOf(openTask)))
        }

        composeTestRule.onNodeWithText("Task Dang Mo").performTouchInput {
            // endX cố ý vượt xa mép trái của node khớp được (Column tiêu đề, hẹp hơn cả Row) để đảm
            // bảo tổng khoảng cách kéo đủ lớn, vượt ngưỡng dismiss nội bộ của SwipeToDismissBox dù
            // node khớp được không phải toàn bộ chiều rộng row.
            swipeLeft(startX = right, endX = left - 1000f)
        }

        composeTestRule.onNodeWithText("Xóa task").assertExists()
        composeTestRule.onNodeWithText("Bạn có chắc muốn xóa task này? Hành động không thể hoàn tác.")
            .assertExists()
    }

    @Test
    fun swipeRoiNhanXoa_taskBienMatKhoiDanhSach() {
        // TC-DEL-01/03.
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(listOf(openTask)))
        }

        composeTestRule.onNodeWithText("Task Dang Mo").performTouchInput {
            swipeLeft(startX = right, endX = left - 1000f)
        }
        // "Xóa" khớp 2 node sau khi swipe: chữ nền của SwipeToDismissBox (không click được) + nút xác
        // nhận trong AlertDialog (có click action) -> lọc thêm hasClickAction() để chỉ nhắm đúng nút.
        composeTestRule.onNode(hasText("Xóa") and hasClickAction()).performClick()

        composeTestRule.onAllNodesWithText("Xóa task").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Task Dang Mo").assertCountEquals(0)
    }

    @Test
    fun swipeRoiNhanHuy_taskVanConTrongDanhSach() {
        // TC-DEL-02.
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(listOf(openTask)))
        }

        composeTestRule.onNodeWithText("Task Dang Mo").performTouchInput {
            swipeLeft(startX = right, endX = left - 1000f)
        }
        composeTestRule.onNodeWithText("Hủy").performClick()

        composeTestRule.onAllNodesWithText("Xóa task").assertCountEquals(0)
        composeTestRule.onNodeWithText("Task Dang Mo").assertExists()
    }

    @Test
    fun chamVaoCheckbox_khongMoManSua() {
        // TC-TOGGLE-06.
        var navigatedToEdit = false
        composeTestRule.setContent {
            TaskListScreen(
                viewModel = createListViewModel(listOf(openTask)),
                onNavigateToEditTask = { navigatedToEdit = true }
            )
        }

        composeTestRule.onAllNodes(isToggleable())[0].performClick()

        assert(!navigatedToEdit) { "Chạm checkbox không được mở màn Sửa" }
    }

    @Test
    fun locDangMo_chiHienTaskDangMo() {
        // TC-FILTER-01.
        val repository = FakeTaskRepositoryFilterAware(listOf(openTask, doneTask))
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(repository))
        }
        composeTestRule.onNodeWithText("Task Dang Mo").assertExists()
        composeTestRule.onNodeWithText("Task Da Xong").assertExists()

        composeTestRule.onNodeWithText("Đang mở").performClick()

        composeTestRule.onNodeWithText("Task Dang Mo").assertExists()
        composeTestRule.onAllNodesWithText("Task Da Xong").assertCountEquals(0)
    }

    @Test
    fun locDaXong_chiHienTaskDaXong() {
        // TC-FILTER-02.
        val repository = FakeTaskRepositoryFilterAware(listOf(openTask, doneTask))
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(repository))
        }

        composeTestRule.onNodeWithText("Đã xong").performClick()

        composeTestRule.onNodeWithText("Task Da Xong").assertExists()
        composeTestRule.onAllNodesWithText("Task Dang Mo").assertCountEquals(0)
    }

    @Test
    fun locRoiVeTatCa_hienLaiToanBoTask() {
        // TC-FILTER-03.
        val repository = FakeTaskRepositoryFilterAware(listOf(openTask, doneTask))
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(repository))
        }

        composeTestRule.onNodeWithText("Đang mở").performClick()
        composeTestRule.onNodeWithText("Tất cả").performClick()

        composeTestRule.onNodeWithText("Task Dang Mo").assertExists()
        composeTestRule.onNodeWithText("Task Da Xong").assertExists()
    }

    @Test
    fun locDangMoKhiKhongCoTaskMo_hienThongBaoRiengKhacEmptyStateChung() {
        // TC-FILTER-04 — chỉ có task done, lọc "Đang mở" phải ra thông điệp riêng theo filter,
        // khác với empty-state "chưa có task nào" (TC-LIST-06).
        val repository = FakeTaskRepositoryFilterAware(listOf(doneTask))
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(repository))
        }

        composeTestRule.onNodeWithText("Đang mở").performClick()

        composeTestRule.onNodeWithText("Không có task nào đang mở.").assertExists()
    }

    @Test
    fun nhanDangXuat_goiCallbackNgayLapTuc_khongHienDialogXacNhan() {
        // TC-LOGOUT-01 — xác nhận deviation #3 (docs/Test_Scenarios_ToDo_Android.md mục 4): app
        // hiện tại KHÔNG có dialog "Bạn có chắc muốn đăng xuất?" như SRS yêu cầu.
        var loggedOut = false
        composeTestRule.setContent {
            TaskListScreen(
                viewModel = createListViewModel(listOf(openTask)),
                onLoggedOut = { loggedOut = true }
            )
        }

        composeTestRule.onNodeWithText("Đăng xuất").performClick()
        composeTestRule.waitForIdle()

        assert(loggedOut) { "onLoggedOut phải được gọi ngay sau khi nhấn Đăng xuất" }
        composeTestRule.onAllNodesWithText("Hủy").assertCountEquals(0)
    }

    // --- FR-24 (docs/Task_Category_ToDo.md mục 6.4) — badge danh mục trên TaskRow ---

    @Test
    fun taskCoDanhMuc_thiHienBadgeDanhMucTrenRow() {
        val category = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")
        val taskWithCategory = openTask.copy(categoryId = "cat-1")
        composeTestRule.setContent {
            TaskListScreen(
                viewModel = createListViewModel(
                    taskRepository = FakeTaskRepositoryWithFixedList(listOf(taskWithCategory)),
                    categoryRepository = FakeCategoryRepository(listOf(category))
                )
            )
        }

        composeTestRule.onNodeWithText("💼 Công việc").assertExists()
    }

    @Test
    fun taskKhongCoDanhMuc_thiKhongHienBadge() {
        val category = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")
        composeTestRule.setContent {
            TaskListScreen(
                viewModel = createListViewModel(
                    taskRepository = FakeTaskRepositoryWithFixedList(listOf(openTask)), // categoryId == null
                    categoryRepository = FakeCategoryRepository(listOf(category))
                )
            )
        }

        composeTestRule.onAllNodesWithText("💼 Công việc").assertCountEquals(0)
    }

    // --- FR-21 (docs/Task_DueDate_ToDo.md mục 5.4) — dòng "Hạn:" trên TaskRow ---

    @Test
    fun taskCoDueDate_thiHienDongHanChot() {
        val taskWithDueDate = openTask.copy(dueDate = LocalDate.of(2026, 12, 25))
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(listOf(taskWithDueDate)))
        }

        composeTestRule.onNodeWithText("Hạn: 25/12/2026").assertExists()
    }

    @Test
    fun taskKhongCoDueDate_thiKhongHienDongHan() {
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(listOf(openTask))) // dueDate == null
        }

        composeTestRule.onNode(hasText("Hạn:", substring = true)).assertDoesNotExist()
    }

    // --- FR-26 (docs/Task_Reorder_ToDo.md mục 6.2) — cài đặt vị trí thêm task mới ---

    @Test
    fun nhanIconCaiDat_thiHienDialogChonViTriChenTaskMoi() {
        composeTestRule.setContent {
            TaskListScreen(viewModel = createListViewModel(listOf(openTask)))
        }

        composeTestRule.onNodeWithContentDescription("Cài đặt").performClick()

        composeTestRule.onNodeWithText("Thêm task mới vào").assertExists()
        composeTestRule.onNodeWithText("Đầu danh sách").assertExists()
        composeTestRule.onNodeWithText("Cuối danh sách").assertExists()
    }

    @Test
    fun chonCuoiDanhSach_thiLuuPreferenceVaDongDialog() {
        val preferencesRepository = FakePreferencesRepository()
        composeTestRule.setContent {
            TaskListScreen(
                viewModel = createListViewModel(
                    taskRepository = FakeTaskRepositoryWithFixedList(listOf(openTask)),
                    preferencesRepository = preferencesRepository
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Cài đặt").performClick()
        composeTestRule.onNodeWithText("Cuối danh sách").performClick()

        assertEquals(InsertPosition.BOTTOM, preferencesRepository.lastSetPosition)
        // onInsertPositionSelected set showInsertPositionDialog = false ngay -> dialog phải đóng.
        composeTestRule.onAllNodesWithText("Thêm task mới vào").assertCountEquals(0)
    }
}
