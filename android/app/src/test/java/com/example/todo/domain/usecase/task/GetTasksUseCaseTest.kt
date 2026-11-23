package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskFilter
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
 * Unit test cho [GetTasksUseCase] — theo LLD mục 4.1: map [TaskFilter.ALL] -> `null` khi gọi
 * Repository (API_Contract mục 1.7: vắng mặt query `status` nghĩa là "all", KHÔNG gửi
 * `status=all` thẳng ra).
 */
class GetTasksUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val useCase = GetTasksUseCase(taskRepository)

    private val tasks = listOf(
        Task(
            id = "task-1",
            title = "Don nha",
            description = null,
            status = TaskStatus.OPEN,
            createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
            updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
        )
    )

    @Test
    fun `filter ALL thi goi Repository voi status null`() = runTest {
        coEvery { taskRepository.getTasks(null) } returns ApiResult.Success(tasks)

        val result = useCase(TaskFilter.ALL)

        assertEquals(ApiResult.Success(tasks), result)
        coVerify(exactly = 1) { taskRepository.getTasks(null) }
    }

    @Test
    fun `filter OPEN thi goi Repository voi status OPEN`() = runTest {
        coEvery { taskRepository.getTasks(TaskStatus.OPEN) } returns ApiResult.Success(tasks)

        val result = useCase(TaskFilter.OPEN)

        assertEquals(ApiResult.Success(tasks), result)
        coVerify(exactly = 1) { taskRepository.getTasks(TaskStatus.OPEN) }
    }

    @Test
    fun `filter DONE thi goi Repository voi status DONE`() = runTest {
        coEvery { taskRepository.getTasks(TaskStatus.DONE) } returns ApiResult.Success(tasks)

        val result = useCase(TaskFilter.DONE)

        assertEquals(ApiResult.Success(tasks), result)
        coVerify(exactly = 1) { taskRepository.getTasks(TaskStatus.DONE) }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error, khong nuot loi`() = runTest {
        coEvery { taskRepository.getTasks(null) } returns
            ApiResult.Error(ApiErrorCode.SERVER_ERROR, "Lỗi hệ thống")

        val result = useCase(TaskFilter.ALL)

        assertEquals(ApiResult.Error(ApiErrorCode.SERVER_ERROR, "Lỗi hệ thống"), result)
    }
}
