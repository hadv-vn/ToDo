package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit test cho [UpdateTaskUseCase] — theo LLD mục 4.1: validate title.trim().isNotEmpty() mang
 * tính defensive TRƯỚC khi gọi Repository.editTask (KHÔNG đổi status — xem ToggleTaskStatusUseCase
 * cho nhánh đổi status, LLD mục 3.4/9.3).
 */
class UpdateTaskUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val useCase = UpdateTaskUseCase(taskRepository)

    private val updatedTask = Task(
        id = "task-1",
        title = "Task da sua",
        description = "mo ta moi",
        status = TaskStatus.OPEN,
        createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
        updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
    )

    @Test
    fun `title rong thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("task-1", "", "mo ta")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { taskRepository.editTask(any(), any(), any()) }
    }

    @Test
    fun `title chi co khoang trang thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("task-1", "\t\n ", "mo ta")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { taskRepository.editTask(any(), any(), any()) }
    }

    @Test
    fun `title hop le thi goi Repository editTask voi title da trim, khong doi status`() = runTest {
        coEvery { taskRepository.editTask("task-1", "Task da sua", "mo ta moi") } returns
            ApiResult.Success(updatedTask)

        val result = useCase("task-1", "  Task da sua  ", "mo ta moi")

        assertEquals(ApiResult.Success(updatedTask), result)
        coVerify(exactly = 1) { taskRepository.editTask("task-1", "Task da sua", "mo ta moi") }
        coVerify(exactly = 0) { taskRepository.toggleTaskStatus(any(), any()) }
    }

    @Test
    fun `description chi co khoang trang thi gui null cho Repository`() = runTest {
        coEvery { taskRepository.editTask("task-1", "Task da sua", null) } returns ApiResult.Success(updatedTask)

        val result = useCase("task-1", "Task da sua", "   ")

        assertEquals(ApiResult.Success(updatedTask), result)
        coVerify(exactly = 1) { taskRepository.editTask("task-1", "Task da sua", null) }
    }
}
