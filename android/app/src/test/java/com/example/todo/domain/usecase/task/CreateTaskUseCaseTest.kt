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
 * Unit test cho [CreateTaskUseCase] — theo LLD mục 4.1: validate title.trim().isNotEmpty() mang
 * tính defensive TRƯỚC khi gọi Repository.
 */
class CreateTaskUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val useCase = CreateTaskUseCase(taskRepository)

    private val createdTask = Task(
        id = "task-1",
        title = "Task moi",
        description = null,
        status = TaskStatus.OPEN,
        createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
        updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
    )

    @Test
    fun `title rong thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("", "mo ta")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { taskRepository.createTask(any(), any()) }
    }

    @Test
    fun `title chi co khoang trang thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("   ", null)

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { taskRepository.createTask(any(), any()) }
    }

    @Test
    fun `title hop le thi goi Repository voi title da trim`() = runTest {
        coEvery { taskRepository.createTask("Task moi", "mo ta") } returns ApiResult.Success(createdTask)

        val result = useCase("  Task moi  ", "mo ta")

        assertEquals(ApiResult.Success(createdTask), result)
        coVerify(exactly = 1) { taskRepository.createTask("Task moi", "mo ta") }
    }

    @Test
    fun `description chi co khoang trang thi gui null cho Repository`() = runTest {
        coEvery { taskRepository.createTask("Task moi", null) } returns ApiResult.Success(createdTask)

        val result = useCase("Task moi", "   ")

        assertEquals(ApiResult.Success(createdTask), result)
        coVerify(exactly = 1) { taskRepository.createTask("Task moi", null) }
    }
}
