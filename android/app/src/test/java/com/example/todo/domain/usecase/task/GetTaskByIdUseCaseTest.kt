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
 * Unit test cho [GetTaskByIdUseCase] — pass-through, không có rule nghiệp vụ ở tầng này (dùng để
 * load dữ liệu cho EditTaskScreen qua GET /tasks/:id).
 */
class GetTaskByIdUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val useCase = GetTaskByIdUseCase(taskRepository)

    private val task = Task(
        id = "task-1",
        title = "Don nha",
        description = null,
        status = TaskStatus.OPEN,
        createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
        updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
    )

    @Test
    fun `goi dung Repository voi id va tra Success Task`() = runTest {
        coEvery { taskRepository.getTaskById("task-1") } returns ApiResult.Success(task)

        val result = useCase("task-1")

        assertEquals(ApiResult.Success(task), result)
        coVerify(exactly = 1) { taskRepository.getTaskById("task-1") }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error`() = runTest {
        coEvery { taskRepository.getTaskById("task-404") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")

        val result = useCase("task-404")

        assertEquals(ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại"), result)
    }
}
