package com.example.todo.presentation.taskform

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        // Lưu lại task vừa tạo để test xác nhận đúng categoryId/dueDate/priority đã gửi — xem các
        // test chọn danh mục/mức ưu tiên/hạn chót ở dưới.
        var lastCreatedTask: Task? = null

        override suspend fun getTasks(filter: TaskStatus?) = throw NotImplementedError()
        override suspend fun getTaskById(id: String) = throw NotImplementedError()
        override suspend fun createTask(
            title: String,
            description: String?,
            insertPosition: InsertPosition,
            categoryId: String?,
            dueDate: java.time.LocalDate?,
            priority: com.example.todo.domain.model.TaskPriority
        ): ApiResult<Task> {
            val created = Task(
                id = "new-task",
                title = title,
                description = description,
                status = TaskStatus.OPEN,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                categoryId = categoryId,
                dueDate = dueDate,
                priority = priority
            )
            lastCreatedTask = created
            return ApiResult.Success(created)
        }
        override suspend fun editTask(
            id: String,
            title: String,
            description: String?,
            categoryId: String?,
            dueDate: java.time.LocalDate?,
            priority: com.example.todo.domain.model.TaskPriority
        ) = throw NotImplementedError()
        override suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus) = throw NotImplementedError()
        override suspend fun reorderTask(id: String, afterTaskId: String?, beforeTaskId: String?) =
            throw NotImplementedError()
        override suspend fun deleteTask(id: String) = throw NotImplementedError()
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

    private fun createFormViewModel(
        taskRepository: FakeTaskRepository = FakeTaskRepository(),
        categories: List<Category> = emptyList()
    ): TaskFormViewModel {
        return TaskFormViewModel(
            savedStateHandle = SavedStateHandle(), // taskId == null -> Create mode
            getTaskByIdUseCase = GetTaskByIdUseCase(taskRepository),
            createTaskUseCase = CreateTaskUseCase(taskRepository),
            updateTaskUseCase = UpdateTaskUseCase(taskRepository),
            getInsertPositionPreferenceUseCase = GetInsertPositionPreferenceUseCase(FakePreferencesRepository()),
            getCategoriesUseCase = GetCategoriesUseCase(FakeCategoryRepository(categories))
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

    // --- FR-18 (docs/Task_Priority_ToDo.md) — chọn mức ưu tiên ---

    @Test
    fun chonMucUuTienCao_thiTaoTaskVoiPriorityHigh() {
        val repository = FakeTaskRepository()
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel(taskRepository = repository))
        }

        composeTestRule.onNodeWithText("Tiêu đề").performTextInput("Task moi")
        composeTestRule.onNodeWithText("Cao").performClick()
        composeTestRule.onNodeWithText("Lưu").performClick()

        assertEquals(TaskPriority.HIGH, repository.lastCreatedTask?.priority)
    }

    @Test
    fun khongChonMucUuTien_thiTaoTaskVoiPriorityMacDinhTrungBinh() {
        val repository = FakeTaskRepository()
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel(taskRepository = repository))
        }

        composeTestRule.onNodeWithText("Tiêu đề").performTextInput("Task moi")
        composeTestRule.onNodeWithText("Lưu").performClick()

        assertEquals(TaskPriority.MEDIUM, repository.lastCreatedTask?.priority)
    }

    // --- FR-23 (docs/Task_Category_ToDo.md) — chọn danh mục ---

    @Test
    fun coDanhMucKhaDung_thiHienChipVaChonDuocDanhMuc() {
        val repository = FakeTaskRepository()
        val category = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel(taskRepository = repository, categories = listOf(category)))
        }

        composeTestRule.onNodeWithText("Tiêu đề").performTextInput("Task moi")
        composeTestRule.onNodeWithText("💼 Công việc").performClick()
        composeTestRule.onNodeWithText("Lưu").performClick()

        assertEquals("cat-1", repository.lastCreatedTask?.categoryId)
    }

    @Test
    fun khongChonDanhMuc_thiTaoTaskVoiCategoryIdNull() {
        val repository = FakeTaskRepository()
        val category = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel(taskRepository = repository, categories = listOf(category)))
        }

        composeTestRule.onNodeWithText("Tiêu đề").performTextInput("Task moi")
        composeTestRule.onNodeWithText("Lưu").performClick()

        assertEquals(null, repository.lastCreatedTask?.categoryId)
    }

    @Test
    fun chuaCoDanhMucNao_thiHienChipTaoDanhMucVaGoiCallback() {
        var navigatedToCategoryList = false
        composeTestRule.setContent {
            CreateTaskScreen(
                viewModel = createFormViewModel(),
                onNavigateToCategoryList = { navigatedToCategoryList = true }
            )
        }

        composeTestRule.onNodeWithText("+ Tạo danh mục").performClick()

        assertTrue(navigatedToCategoryList)
    }

    // --- FR-20 (docs/Task_DueDate_ToDo.md) — chọn hạn chót ---

    @Test
    fun bamThemHanChot_thiMoDialogChonNgay_bamHuy_thiDongDialogKhongDoiDueDate() {
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel())
        }

        composeTestRule.onNodeWithText("+ Thêm hạn chót").performClick()
        composeTestRule.onNodeWithText("Chọn").assertIsDisplayed()

        composeTestRule.onNodeWithText("Hủy").performClick()

        // Dialog đã đóng, vẫn chưa có hạn chót -> nút "+ Thêm hạn chót" hiện lại.
        composeTestRule.onNodeWithText("+ Thêm hạn chót").assertIsDisplayed()
    }

    @Test
    fun khongChonHanChot_thiTaoTaskVoiDueDateNull() {
        val repository = FakeTaskRepository()
        composeTestRule.setContent {
            CreateTaskScreen(viewModel = createFormViewModel(taskRepository = repository))
        }

        composeTestRule.onNodeWithText("Tiêu đề").performTextInput("Task moi")
        composeTestRule.onNodeWithText("Lưu").performClick()

        assertEquals(null, repository.lastCreatedTask?.dueDate)
    }
}
