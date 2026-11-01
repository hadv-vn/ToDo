package com.example.todo.presentation.tasklist

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.repository.AuthRepository
import com.example.todo.domain.repository.TaskRepository
import com.example.todo.domain.usecase.auth.LogoutUseCase
import com.example.todo.domain.usecase.task.DeleteTaskUseCase
import com.example.todo.domain.usecase.task.GetTasksUseCase
import com.example.todo.domain.usecase.task.ToggleTaskStatusUseCase
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Compose UI test cho luồng danh sách Task (LLD mục 6.4/8) — checkbox phản ánh đúng trạng thái
 * done/open, và swipe trên 1 row mở ra dialog xác nhận xóa.
 *
 * Không dùng MockK (androidTestImplementation chưa có `mockk-android`) — dùng
 * [FakeTaskRepositoryWithFixedList]/[FakeAuthRepositoryLogoutOnly] viết tay implement interface.
 *
 * LƯU Ý: máy phát triển hiện tại KHÔNG có Android SDK/emulator, `./gradlew :app:connectedAndroidTest`
 * CHƯA thể chạy thật. Test swipe-to-delete đặc biệt phụ thuộc vào ngưỡng kéo (positionalThreshold)
 * nội bộ của `SwipeToDismissBox` (Material3) — đã cố ý truyền `endX` vượt xa mép trái của node khớp
 * được để tối đa hóa khoảng cách kéo, nhưng CHƯA thể xác nhận chắc chắn bằng cách chạy thật trên
 * thiết bị/emulator. Nếu chạy thật mà thất bại, cần tinh chỉnh toạ độ/thời lượng swipe.
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
        override suspend fun createTask(title: String, description: String?) = throw NotImplementedError()
        override suspend fun editTask(id: String, title: String, description: String?) = throw NotImplementedError()
        override suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus) = throw NotImplementedError()
        override suspend fun deleteTask(id: String): ApiResult<Unit> = ApiResult.Success(Unit)
    }

    private class FakeAuthRepositoryLogoutOnly : AuthRepository {
        override suspend fun register(email: String, password: String, confirmPassword: String) =
            throw NotImplementedError()
        override suspend fun login(email: String, password: String) = throw NotImplementedError()
        override suspend fun logout() = Unit
        override suspend fun forgotPassword(email: String) = throw NotImplementedError()
        override suspend fun resetPassword(
            email: String,
            otp: String,
            newPassword: String,
            confirmNewPassword: String
        ) = throw NotImplementedError()
        override suspend fun hasStoredToken(): Boolean = false
    }

    private fun createListViewModel(tasks: List<Task>): TaskListViewModel {
        val taskRepository = FakeTaskRepositoryWithFixedList(tasks)
        return TaskListViewModel(
            getTasksUseCase = GetTasksUseCase(taskRepository),
            toggleTaskStatusUseCase = ToggleTaskStatusUseCase(taskRepository),
            deleteTaskUseCase = DeleteTaskUseCase(taskRepository),
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
}
