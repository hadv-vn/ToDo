package com.example.todo.presentation.taskform

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskPriority
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.model.Category
import com.example.todo.domain.repository.CategoryRepository
import com.example.todo.domain.repository.PreferencesRepository
import com.example.todo.domain.repository.TaskRepository
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.domain.usecase.preferences.GetInsertPositionPreferenceUseCase
import com.example.todo.domain.usecase.task.CreateTaskUseCase
import com.example.todo.domain.usecase.task.GetTaskByIdUseCase
import com.example.todo.domain.usecase.task.UpdateTaskUseCase
import com.example.todo.presentation.navigation.Routes
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Compose UI test cho luồng Sửa Task (LLD mục 5 "Luồng tải dữ liệu EditTaskScreen", FR-12/SRS).
 *
 * Khác [CreateTaskScreenTest]: [TaskFormViewModel] ở Edit mode (`taskId != null` đọc từ
 * [SavedStateHandle]) tự fetch dữ liệu qua [GetTaskByIdUseCase] ngay khi init -- dùng
 * `CompletableDeferred` để kiểm soát thời điểm kết quả trả về, xác nhận đúng trạng thái loading
 * trước khi load xong (giống kỹ thuật ở LoginScreenTest/RegisterScreenTest, không dùng MockK vì
 * `androidTestImplementation` chưa có `mockk-android`).
 */
class EditTaskScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeTaskRepository(
        private val pendingGetTaskById: CompletableDeferred<ApiResult<Task>>,
        private val pendingEditTask: CompletableDeferred<ApiResult<Task>>? = null
    ) : TaskRepository {
        // Lưu lại tham số editTask nhận được TRƯỚC khi await — test xác nhận đúng
        // categoryId/dueDate/priority mới đã gửi đi, không cần đợi pendingEditTask complete.
        var lastEditCategoryId: String? = null
        var lastEditDueDate: LocalDate? = null
        var lastEditPriority: TaskPriority? = null

        override suspend fun getTasks(filter: TaskStatus?) =
            throw NotImplementedError("Không dùng trong EditTaskScreenTest")

        override suspend fun getTaskById(id: String): ApiResult<Task> = pendingGetTaskById.await()

        override suspend fun createTask(
            title: String,
            description: String?,
            insertPosition: InsertPosition,
            categoryId: String?,
            dueDate: java.time.LocalDate?,
            priority: com.example.todo.domain.model.TaskPriority
        ) = throw NotImplementedError("Không dùng trong EditTaskScreenTest")

        override suspend fun editTask(
            id: String,
            title: String,
            description: String?,
            categoryId: String?,
            dueDate: java.time.LocalDate?,
            priority: com.example.todo.domain.model.TaskPriority
        ): ApiResult<Task> {
            lastEditCategoryId = categoryId
            lastEditDueDate = dueDate
            lastEditPriority = priority
            return pendingEditTask?.await() ?: throw NotImplementedError("pendingEditTask chưa được cấu hình")
        }

        override suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus) =
            throw NotImplementedError("Không dùng trong EditTaskScreenTest")

        override suspend fun reorderTask(id: String, afterTaskId: String?, beforeTaskId: String?) =
            throw NotImplementedError("Không dùng trong EditTaskScreenTest")

        override suspend fun deleteTask(id: String) =
            throw NotImplementedError("Không dùng trong EditTaskScreenTest")
    }

    private class FakePreferencesRepository : PreferencesRepository {
        override suspend fun getInsertPosition(): InsertPosition = InsertPosition.TOP
        override suspend fun setInsertPosition(position: InsertPosition) = Unit
    }

    private class FakeCategoryRepository(private val categories: List<Category> = emptyList()) : CategoryRepository {
        override suspend fun getCategories(): ApiResult<List<Category>> = ApiResult.Success(categories)
        override suspend fun createCategory(name: String, color: String?, icon: String?) = throw NotImplementedError()
        override suspend fun updateCategory(id: String, name: String, color: String, icon: String) =
            throw NotImplementedError()
        override suspend fun deleteCategory(id: String) = throw NotImplementedError()
    }

    private fun sampleTask(
        title: String,
        description: String?,
        categoryId: String? = null,
        dueDate: LocalDate? = null,
        priority: TaskPriority = TaskPriority.MEDIUM
    ) = Task(
        id = TASK_ID,
        title = title,
        description = description,
        status = TaskStatus.OPEN,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        categoryId = categoryId,
        dueDate = dueDate,
        priority = priority
    )

    private fun createFormViewModel(
        pendingGetTaskById: CompletableDeferred<ApiResult<Task>>,
        pendingEditTask: CompletableDeferred<ApiResult<Task>>? = null,
        categories: List<Category> = emptyList()
    ): TaskFormViewModel {
        val repository = FakeTaskRepository(pendingGetTaskById, pendingEditTask)
        return TaskFormViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_TASK_ARG_TASK_ID to TASK_ID)),
            getTaskByIdUseCase = GetTaskByIdUseCase(repository),
            createTaskUseCase = CreateTaskUseCase(repository),
            updateTaskUseCase = UpdateTaskUseCase(repository),
            getInsertPositionPreferenceUseCase = GetInsertPositionPreferenceUseCase(FakePreferencesRepository()),
            getCategoriesUseCase = GetCategoriesUseCase(FakeCategoryRepository(categories))
        )
    }

    /** Dùng cho test cần giữ tham chiếu [repository] để xác nhận tham số editTask đã gửi. */
    private fun createFormViewModel(
        repository: FakeTaskRepository,
        categories: List<Category> = emptyList()
    ): TaskFormViewModel {
        return TaskFormViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_TASK_ARG_TASK_ID to TASK_ID)),
            getTaskByIdUseCase = GetTaskByIdUseCase(repository),
            createTaskUseCase = CreateTaskUseCase(repository),
            updateTaskUseCase = UpdateTaskUseCase(repository),
            getInsertPositionPreferenceUseCase = GetInsertPositionPreferenceUseCase(FakePreferencesRepository()),
            getCategoriesUseCase = GetCategoriesUseCase(FakeCategoryRepository(categories))
        )
    }

    @Test
    fun dangTaiDuLieu_hienLoading_sauDoDienSanTitleVaDescription() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()

        composeTestRule.setContent {
            EditTaskScreen(viewModel = createFormViewModel(pendingGet))
        }

        // Đang isLoading=true -> form (OutlinedTextField "Tiêu đề") chưa được render.
        composeTestRule.onNodeWithText("Tiêu đề").assertDoesNotExist()

        pendingGet.complete(ApiResult.Success(sampleTask(title = "Task cu", description = "Mo ta cu")))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Task cu").assertExists()
        composeTestRule.onNodeWithText("Mo ta cu").assertExists()
    }

    @Test
    fun taskDaBiXoaORemote_404KhiTaiDuLieu_hienThongBaoVaTuDieuHuongVeTaskList() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()
        var navigatedBack = false

        composeTestRule.setContent {
            EditTaskScreen(
                viewModel = createFormViewModel(pendingGet),
                onSubmitSuccess = { navigatedBack = true }
            )
        }

        pendingGet.complete(ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Task này không còn tồn tại").assertExists()
        // FR-12 (SRS)/LLD mục 5: taskDeletedRemotely dùng chung callback onSubmitSuccess để
        // TaskList tự loadTasks() lại và loại bỏ task đã bị xóa khỏi danh sách.
        assert(navigatedBack) { "Phải tự điều hướng về TaskList (qua onSubmitSuccess) khi task đã bị xóa ở nơi khác" }
    }

    @Test
    fun suaTitleHopLe_luuThanhCong_dieuHuongVeTaskList() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()
        val pendingEdit = CompletableDeferred<ApiResult<Task>>()
        var navigatedBack = false

        composeTestRule.setContent {
            EditTaskScreen(
                viewModel = createFormViewModel(pendingGet, pendingEdit),
                onSubmitSuccess = { navigatedBack = true }
            )
        }

        pendingGet.complete(ApiResult.Success(sampleTask(title = "Task cu", description = null)))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Task cu").performTextClearance()
        composeTestRule.onNodeWithText("Tiêu đề").performTextInput("Task moi")
        composeTestRule.onNodeWithText("Lưu").performClick()

        pendingEdit.complete(ApiResult.Success(sampleTask(title = "Task moi", description = null)))
        composeTestRule.waitForIdle()

        assert(navigatedBack) { "Phải điều hướng về TaskList sau khi lưu sửa task thành công" }
    }

    // --- FR-18/FR-23/FR-20 — nạp sẵn priority/category/dueDate hiện có, sửa rồi lưu đúng giá trị mới ---

    @Test
    fun taskCoSanPriorityHigh_thiChipCaoDuocChonSan() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()
        composeTestRule.setContent {
            EditTaskScreen(viewModel = createFormViewModel(pendingGet))
        }

        pendingGet.complete(ApiResult.Success(sampleTask(title = "Task cu", description = null, priority = TaskPriority.HIGH)))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cao").assertExists()
    }

    @Test
    fun doiMucUuTienSangThap_luuThanhCong_thiGuiDungPriorityMoi() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()
        val pendingEdit = CompletableDeferred<ApiResult<Task>>()
        val repository = FakeTaskRepository(pendingGet, pendingEdit)

        composeTestRule.setContent {
            EditTaskScreen(viewModel = createFormViewModel(repository))
        }

        pendingGet.complete(ApiResult.Success(sampleTask(title = "Task cu", description = null, priority = TaskPriority.MEDIUM)))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Thấp").performClick()
        composeTestRule.onNodeWithText("Lưu").performClick()
        pendingEdit.complete(ApiResult.Success(sampleTask(title = "Task cu", description = null, priority = TaskPriority.LOW)))
        composeTestRule.waitForIdle()

        assertEquals(TaskPriority.LOW, repository.lastEditPriority)
    }

    @Test
    fun taskCoSanCategoryId_thiChipDanhMucTuongUngDuocChonSan() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()
        val category = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")
        composeTestRule.setContent {
            EditTaskScreen(viewModel = createFormViewModel(pendingGet, categories = listOf(category)))
        }

        pendingGet.complete(ApiResult.Success(sampleTask(title = "Task cu", description = null, categoryId = "cat-1")))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("💼 Công việc").assertExists()
    }

    @Test
    fun boDanhMucDangChon_luuThanhCong_thiGuiCategoryIdNull() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()
        val pendingEdit = CompletableDeferred<ApiResult<Task>>()
        val category = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")
        val repository = FakeTaskRepository(pendingGet, pendingEdit)

        composeTestRule.setContent {
            EditTaskScreen(viewModel = createFormViewModel(repository, categories = listOf(category)))
        }

        pendingGet.complete(ApiResult.Success(sampleTask(title = "Task cu", description = null, categoryId = "cat-1")))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Không danh mục").performClick()
        composeTestRule.onNodeWithText("Lưu").performClick()
        pendingEdit.complete(ApiResult.Success(sampleTask(title = "Task cu", description = null, categoryId = null)))
        composeTestRule.waitForIdle()

        assertEquals(null, repository.lastEditCategoryId)
    }

    @Test
    fun taskCoSanDueDate_thiHienDungNgayVaNutXoaHan() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()
        composeTestRule.setContent {
            EditTaskScreen(viewModel = createFormViewModel(pendingGet))
        }

        pendingGet.complete(
            ApiResult.Success(sampleTask(title = "Task cu", description = null, dueDate = LocalDate.of(2026, 12, 25)))
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("25/12/2026").assertExists()
        composeTestRule.onNodeWithText("Xóa hạn").assertExists()
    }

    @Test
    fun xoaHanChotDangCo_luuThanhCong_thiGuiDueDateNull() {
        val pendingGet = CompletableDeferred<ApiResult<Task>>()
        val pendingEdit = CompletableDeferred<ApiResult<Task>>()
        val repository = FakeTaskRepository(pendingGet, pendingEdit)

        composeTestRule.setContent {
            EditTaskScreen(viewModel = createFormViewModel(repository))
        }

        pendingGet.complete(
            ApiResult.Success(sampleTask(title = "Task cu", description = null, dueDate = LocalDate.of(2026, 12, 25)))
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Xóa hạn").performClick()
        composeTestRule.onNodeWithText("Lưu").performClick()
        pendingEdit.complete(ApiResult.Success(sampleTask(title = "Task cu", description = null, dueDate = null)))
        composeTestRule.waitForIdle()

        assertEquals(null, repository.lastEditDueDate)
    }

    private companion object {
        const val TASK_ID = "task-1"
    }
}
