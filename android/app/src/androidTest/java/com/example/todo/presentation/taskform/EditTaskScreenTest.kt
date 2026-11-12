package com.example.todo.presentation.taskform

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.repository.TaskRepository
import com.example.todo.domain.usecase.task.CreateTaskUseCase
import com.example.todo.domain.usecase.task.GetTaskByIdUseCase
import com.example.todo.domain.usecase.task.UpdateTaskUseCase
import com.example.todo.presentation.navigation.Routes
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import java.time.Instant

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
        override suspend fun getTasks(filter: TaskStatus?) =
            throw NotImplementedError("Không dùng trong EditTaskScreenTest")

        override suspend fun getTaskById(id: String): ApiResult<Task> = pendingGetTaskById.await()

        override suspend fun createTask(title: String, description: String?) =
            throw NotImplementedError("Không dùng trong EditTaskScreenTest")

        override suspend fun editTask(id: String, title: String, description: String?): ApiResult<Task> =
            pendingEditTask?.await() ?: throw NotImplementedError("pendingEditTask chưa được cấu hình")

        override suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus) =
            throw NotImplementedError("Không dùng trong EditTaskScreenTest")

        override suspend fun deleteTask(id: String) =
            throw NotImplementedError("Không dùng trong EditTaskScreenTest")
    }

    private fun sampleTask(title: String, description: String?) = Task(
        id = TASK_ID,
        title = title,
        description = description,
        status = TaskStatus.OPEN,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun createFormViewModel(
        pendingGetTaskById: CompletableDeferred<ApiResult<Task>>,
        pendingEditTask: CompletableDeferred<ApiResult<Task>>? = null
    ): TaskFormViewModel {
        val repository = FakeTaskRepository(pendingGetTaskById, pendingEditTask)
        return TaskFormViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_TASK_ARG_TASK_ID to TASK_ID)),
            getTaskByIdUseCase = GetTaskByIdUseCase(repository),
            createTaskUseCase = CreateTaskUseCase(repository),
            updateTaskUseCase = UpdateTaskUseCase(repository)
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

    private companion object {
        const val TASK_ID = "task-1"
    }
}
