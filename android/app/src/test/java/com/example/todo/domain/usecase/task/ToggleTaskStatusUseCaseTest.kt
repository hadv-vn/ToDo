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
import org.junit.Test
import java.time.Instant

/**
 * Unit test cho [ToggleTaskStatusUseCase] — theo LLD mục 4.1: tự tính
 * `newStatus = opposite(currentStatus)` rồi gọi `TaskRepository.toggleTaskStatus`. Double-tap
 * protection/optimistic update/rollback thuộc trách nhiệm ViewModel (mục 4.4a), không test ở đây.
 */
class ToggleTaskStatusUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val useCase = ToggleTaskStatusUseCase(taskRepository)

    private fun task(status: TaskStatus) = Task(
        id = "task-1",
        title = "Don nha",
        description = null,
        status = status,
        createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
        updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
    )

    @Test
    fun `currentStatus OPEN thi goi Repository voi newStatus DONE`() = runTest {
        coEvery { taskRepository.toggleTaskStatus("task-1", TaskStatus.DONE) } returns
            ApiResult.Success(task(TaskStatus.DONE))

        val result = useCase("task-1", TaskStatus.OPEN)

        assertEquals(ApiResult.Success(task(TaskStatus.DONE)), result)
        coVerify(exactly = 1) { taskRepository.toggleTaskStatus("task-1", TaskStatus.DONE) }
    }

    @Test
    fun `currentStatus DONE thi goi Repository voi newStatus OPEN`() = runTest {
        coEvery { taskRepository.toggleTaskStatus("task-1", TaskStatus.OPEN) } returns
            ApiResult.Success(task(TaskStatus.OPEN))

        val result = useCase("task-1", TaskStatus.DONE)

        assertEquals(ApiResult.Success(task(TaskStatus.OPEN)), result)
        coVerify(exactly = 1) { taskRepository.toggleTaskStatus("task-1", TaskStatus.OPEN) }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error, khong nuot loi`() = runTest {
        coEvery { taskRepository.toggleTaskStatus("task-404", TaskStatus.DONE) } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")

        val result = useCase("task-404", TaskStatus.OPEN)

        assertEquals(ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại"), result)
    }
}
