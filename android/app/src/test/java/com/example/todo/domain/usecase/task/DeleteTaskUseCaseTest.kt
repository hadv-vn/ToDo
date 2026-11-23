package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit test cho [DeleteTaskUseCase] — pass-through, không có rule nghiệp vụ ở tầng này. */
class DeleteTaskUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val useCase = DeleteTaskUseCase(taskRepository)

    @Test
    fun `goi dung Repository voi id va tra Success Unit`() = runTest {
        coEvery { taskRepository.deleteTask("task-1") } returns ApiResult.Success(Unit)

        val result = useCase("task-1")

        assertEquals(ApiResult.Success(Unit), result)
        coVerify(exactly = 1) { taskRepository.deleteTask("task-1") }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error`() = runTest {
        coEvery { taskRepository.deleteTask("task-404") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")

        val result = useCase("task-404")

        assertEquals(ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại"), result)
    }
}
