package com.example.todo.presentation.taskform

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.repository.TaskRepository
import com.example.todo.domain.usecase.task.CreateTaskUseCase
import com.example.todo.domain.usecase.task.GetTaskByIdUseCase
import com.example.todo.domain.usecase.task.UpdateTaskUseCase
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Compose UI test cho luồng Tạo Task (LLD mục 6.3/8).
 *
 * BẤT THƯỜNG ĐÃ PHÁT HIỆN (ghi lại theo nguyên tắc #11, KHÔNG tự sửa code sản phẩm): kịch bản gốc
 * được giao là "nhập title rỗng -> nhấn Lưu -> lỗi inline hiển thị, nút Lưu không bị mất tương
 * tác". Đọc [CreateTaskScreen] thực tế cho thấy nút "Lưu" có
 * `enabled = !uiState.isSubmitting && uiState.title.isNotBlank()` — khi title rỗng/chỉ-khoảng-trắng,
 * nút "Lưu" bị DISABLE ngay (không thể bấm được) THAY VÌ cho phép bấm rồi mới hiện lỗi inline từ
 * `TaskFormViewModel.onSubmit()`. Nhánh validate `title.trim().isEmpty()` trong `onSubmit()` do đó
 * KHÔNG thể được kích hoạt qua tương tác UI thực tế ở CreateTaskScreen (nhánh này vẫn có ý nghĩa
 * khi gọi trực tiếp lên ViewModel — xem TaskFormViewModelTest). Test dưới đây xác nhận ĐÚNG hành vi
 * THẬT đang chạy (nút tự disable), không giả định hành vi mô tả ban đầu.
 *
 * Dùng `onNodeWithText("Lưu")` (KHÔNG kết hợp `hasClickAction()`): khi Button bị `enabled=false`,
 * Compose Foundation KHÔNG gắn `SemanticsActions.OnClick` vào node nữa (chỉ gắn property
 * `Disabled`) — nên matcher kết hợp `hasClickAction()` sẽ không khớp node nào lúc disabled và làm
 * assertion thất bại sai cách (không tìm thấy node thay vì "node tồn tại nhưng disabled").
 * `assertIsEnabled()/assertIsNotEnabled()` chỉ dựa vào có/không có property `Disabled`, không phụ
 * thuộc `OnClick`, nên dùng được cho cả 2 trạng thái.
 *
 * Không dùng MockK (androidTestImplementation chưa có `mockk-android`) — dùng [FakeTaskRepository]
 * viết tay implement [TaskRepository].
 *
 * LƯU Ý: máy phát triển hiện tại KHÔNG có Android SDK/emulator, `./gradlew :app:connectedAndroidTest`
 * CHƯA thể chạy thật. Test đã được rà soát thủ công kỹ nhưng CHƯA verify bằng cách chạy thật.
 */
class CreateTaskScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeTaskRepository : TaskRepository {
        override suspend fun getTasks(filter: TaskStatus?) = throw NotImplementedError()
        override suspend fun getTaskById(id: String) = throw NotImplementedError()
        override suspend fun createTask(title: String, description: String?): ApiResult<Task> {
            return ApiResult.Success(
                Task(
                    id = "new-task",
                    title = title,
                    description = description,
                    status = TaskStatus.OPEN,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        override suspend fun editTask(id: String, title: String, description: String?) = throw NotImplementedError()
        override suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus) = throw NotImplementedError()
        override suspend fun deleteTask(id: String) = throw NotImplementedError()
    }

    private fun createFormViewModel(): TaskFormViewModel {
        val repository = FakeTaskRepository()
        return TaskFormViewModel(
            savedStateHandle = SavedStateHandle(), // taskId == null -> Create mode
            getTaskByIdUseCase = GetTaskByIdUseCase(repository),
            createTaskUseCase = CreateTaskUseCase(repository),
            updateTaskUseCase = UpdateTaskUseCase(repository)
        )
    }

    @Test
    fun titleRong_thiNutLuuBiDisable_khongTheBamDuocDeGuiDuLieuSai() {
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel())
        }

        // Title mặc định rỗng -> nút "Lưu" phải đang bị disable.
        composeTestRule.onNodeWithText("Lưu").assertIsNotEnabled()
    }

    @Test
    fun nhapTitleHopLe_thiNutLuuDuocBatLai() {
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel())
        }

        composeTestRule.onNodeWithText("Lưu").assertIsNotEnabled()

        composeTestRule.onNodeWithText("Tiêu đề").performTextInput("Task moi")

        composeTestRule.onNodeWithText("Lưu").assertIsEnabled()
    }

    @Test
    fun nhapTitleRoiXoaHetVeRong_thiNutLuuTuDongDisableLai() {
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel())
        }

        composeTestRule.onNodeWithText("Tiêu đề").performTextInput("A")
        composeTestRule.onNodeWithText("Lưu").assertIsEnabled()

        // Xóa hết text đã nhập -> title rỗng trở lại -> nút phải tự disable (phản ứng theo state).
        composeTestRule.onNodeWithText("A").performTextClearance()
        composeTestRule.onNodeWithText("Lưu").assertIsNotEnabled()
    }
}
