package com.example.todo.presentation.taskform

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.usecase.task.CreateTaskUseCase
import com.example.todo.domain.usecase.task.GetTaskByIdUseCase
import com.example.todo.domain.usecase.task.UpdateTaskUseCase
import com.example.todo.presentation.navigation.Routes
import io.mockk.coAnswers
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
 * Unit test cho [TaskFormViewModel] — dùng chung cho CreateTaskScreen (taskId==null) và
 * EditTaskScreen (taskId!=null). Theo LLD mục 4.2/4.4(b)/mục 5.
 *
 * Lưu ý kỹ thuật: `title`/`description` được lưu trong SavedStateHandle dưới key private
 * "title"/"description" (xem TaskFormViewModel.KEY_TITLE/KEY_DESCRIPTION) — test mô phỏng tình
 * huống "rotation/process death đã khôi phục dữ liệu" bằng cách set thẳng các key literal này vào
 * SavedStateHandle TRƯỚC khi khởi tạo ViewModel, vì đây là cách DUY NHẤT để chuẩn bị trạng thái đó
 * từ bên ngoài (không có setter public nào khác tương đương hành vi OS khôi phục SavedStateHandle).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getTaskByIdUseCase: GetTaskByIdUseCase = mockk()
    private val createTaskUseCase: CreateTaskUseCase = mockk()
    private val updateTaskUseCase: UpdateTaskUseCase = mockk()

    private val existingTask = Task(
        id = "task-1",
        title = "Mua sữa",
        description = "2 lít",
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

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): TaskFormViewModel {
        return TaskFormViewModel(savedStateHandle, getTaskByIdUseCase, createTaskUseCase, updateTaskUseCase)
    }

    // --- Create mode (taskId == null) ---

    @Test
    fun `Create mode title rong thi khong goi UseCase va hien loi inline`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChange("   ") // chỉ khoảng trắng

        viewModel.onSubmit()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Tiêu đề không được để trống", state.titleError)
        }
        coVerify(exactly = 0) { createTaskUseCase(any(), any()) }
        coVerify(exactly = 0) { getTaskByIdUseCase(any()) }
    }

    @Test
    fun `Create mode title hop le thi goi CreateTaskUseCase va set isSubmitSuccess true`() = runTest(testDispatcher) {
        coEvery { createTaskUseCase("Task moi", "Chi tiet") } returns ApiResult.Success(existingTask)

        val viewModel = createViewModel()
        viewModel.onTitleChange("Task moi")
        viewModel.onDescriptionChange("Chi tiet")

        viewModel.uiState.test {
            awaitItem() // sau onDescriptionChange

            viewModel.onSubmit()

            val submitting = awaitItem()
            assertTrue(submitting.isSubmitting)

            val success = awaitItem()
            assertTrue(success.isSubmitSuccess)
            assertFalse(success.isSubmitting)
        }
        coVerify(exactly = 1) { createTaskUseCase("Task moi", "Chi tiet") }
        coVerify(exactly = 0) { updateTaskUseCase(any(), any(), any()) }
    }

    @Test
    fun `Create mode goi onSubmit 2 lan lien tiep thi chi goi UseCase 1 lan`() = runTest(testDispatcher) {
        val pending = CompletableDeferred<ApiResult<Task>>()
        coEvery { createTaskUseCase("Task moi", null) } coAnswers { pending.await() }

        val viewModel = createViewModel()
        viewModel.onTitleChange("Task moi")

        viewModel.onSubmit() // lần 1 -> isSubmitting=true, coroutine suspend
        viewModel.onSubmit() // lần 2 ngay sau đó -> bị chặn

        coVerify(exactly = 1) { createTaskUseCase("Task moi", null) }
        pending.complete(ApiResult.Success(existingTask))
    }

    // --- Edit mode (taskId != null) ---

    @Test
    fun `Edit mode init goi GetTaskByIdUseCase dung 1 lan va set title description`() = runTest(testDispatcher) {
        coEvery { getTaskByIdUseCase("task-1") } returns ApiResult.Success(existingTask)
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_TASK_ARG_TASK_ID to "task-1"))

        val viewModel = createViewModel(savedStateHandle)

        viewModel.uiState.test {
            val afterLoad = awaitItem()
            assertFalse(afterLoad.isLoading)
            assertEquals("Mua sữa", afterLoad.title)
            assertEquals("2 lít", afterLoad.description)
        }
        coVerify(exactly = 1) { getTaskByIdUseCase("task-1") }
    }

    @Test
    fun `Edit mode init nhan Error NOT_FOUND thi set taskDeletedRemotely true`() = runTest(testDispatcher) {
        coEvery { getTaskByIdUseCase("task-404") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_TASK_ARG_TASK_ID to "task-404"))

        val viewModel = createViewModel(savedStateHandle)

        viewModel.uiState.test {
            val afterLoad = awaitItem()
            assertFalse(afterLoad.isLoading)
            assertTrue(afterLoad.taskDeletedRemotely)
        }
    }

    @Test
    fun `Edit mode da co title tu truoc (rotation) thi khong goi lai GetTaskByIdUseCase`() = runTest(testDispatcher) {
        // Mô phỏng ViewModel đã sống sót qua rotation: SavedStateHandle đã có "title" không rỗng
        // từ trước -> init{} phải bỏ qua việc fetch lại (xem TaskFormViewModel.init, điều kiện
        // `taskId != null && uiState.value.title.isEmpty()`).
        val savedStateHandle = SavedStateHandle(
            mapOf(
                Routes.EDIT_TASK_ARG_TASK_ID to "task-1",
                "title" to "Mua sữa", // key literal trùng TaskFormViewModel.KEY_TITLE (private)
                "description" to "2 lít"
            )
        )

        val viewModel = createViewModel(savedStateHandle)

        assertEquals("Mua sữa", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 0) { getTaskByIdUseCase(any()) }
    }

    @Test
    fun `Edit mode onSubmit thanh cong thi goi UpdateTaskUseCase voi dung taskId`() = runTest(testDispatcher) {
        coEvery { getTaskByIdUseCase("task-1") } returns ApiResult.Success(existingTask)
        coEvery { updateTaskUseCase("task-1", "Mua sữa tươi", "2 lít") } returns
            ApiResult.Success(existingTask.copy(title = "Mua sữa tươi"))
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_TASK_ARG_TASK_ID to "task-1"))
        val viewModel = createViewModel(savedStateHandle)

        viewModel.uiState.test {
            awaitItem() // sau init load xong

            viewModel.onTitleChange("Mua sữa tươi")
            awaitItem() // sau onTitleChange

            viewModel.onSubmit()
            val submitting = awaitItem()
            assertTrue(submitting.isSubmitting)

            val success = awaitItem()
            assertTrue(success.isSubmitSuccess)
        }
        coVerify(exactly = 1) { updateTaskUseCase("task-1", "Mua sữa tươi", "2 lít") }
        coVerify(exactly = 0) { createTaskUseCase(any(), any()) }
    }

    @Test
    fun `Edit mode onSubmit nhan Error NOT_FOUND thi set taskDeletedRemotely true`() = runTest(testDispatcher) {
        coEvery { getTaskByIdUseCase("task-1") } returns ApiResult.Success(existingTask)
        coEvery { updateTaskUseCase("task-1", "Mua sữa", "2 lít") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_TASK_ARG_TASK_ID to "task-1"))
        val viewModel = createViewModel(savedStateHandle)

        viewModel.uiState.test {
            awaitItem() // sau init load xong

            viewModel.onSubmit()
            awaitItem() // isSubmitting = true

            val afterNotFound = awaitItem()
            assertTrue(afterNotFound.taskDeletedRemotely)
            assertFalse(afterNotFound.isSubmitting)
        }
    }
}
