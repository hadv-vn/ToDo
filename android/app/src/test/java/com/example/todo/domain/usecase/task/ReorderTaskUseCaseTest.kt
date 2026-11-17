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
 * Unit test cho [ReorderTaskUseCase] — FR-25 (docs/Task_Reorder_ToDo.md mục 3): không có rule
 * nghiệp vụ nào cần validate ở tầng này, chỉ gọi thẳng Repository (cùng pattern [DeleteTaskUseCase]).
 */
class ReorderTaskUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private val useCase = ReorderTaskUseCase(taskRepository)

    private val reorderedTask = Task(
        id = "task-2",
        title = "Don nha",
        description = null,
        status = TaskStatus.OPEN,
        createdAt = Instant.parse("2026-10-14T08:30:00.000Z"),
        updatedAt = Instant.parse("2026-10-14T08:30:00.000Z")
    )

    @Test
    fun `goi dung Repository voi id afterTaskId va beforeTaskId`() = runTest {
        coEvery { taskRepository.reorderTask("task-2", "task-1", "task-3") } returns
            ApiResult.Success(reorderedTask)

        val result = useCase("task-2", "task-1", "task-3")

        assertEquals(ApiResult.Success(reorderedTask), result)
        coVerify(exactly = 1) { taskRepository.reorderTask("task-2", "task-1", "task-3") }
    }

    @Test
    fun `afterTaskId null nghia la keo vao dau danh sach`() = runTest {
        coEvery { taskRepository.reorderTask("task-2", null, "task-3") } returns
            ApiResult.Success(reorderedTask)

        val result = useCase("task-2", null, "task-3")

        assertEquals(ApiResult.Success(reorderedTask), result)
        coVerify(exactly = 1) { taskRepository.reorderTask("task-2", null, "task-3") }
    }

    @Test
    fun `beforeTaskId null nghia la keo vao cuoi danh sach`() = runTest {
        coEvery { taskRepository.reorderTask("task-2", "task-1", null) } returns
            ApiResult.Success(reorderedTask)

        val result = useCase("task-2", "task-1", null)

        assertEquals(ApiResult.Success(reorderedTask), result)
        coVerify(exactly = 1) { taskRepository.reorderTask("task-2", "task-1", null) }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error, khong nuot loi`() = runTest {
        coEvery { taskRepository.reorderTask("task-2", "task-1", "task-3") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại")

        val result = useCase("task-2", "task-1", "task-3")

        assertEquals(ApiResult.Error(ApiErrorCode.NOT_FOUND, "Task không tồn tại"), result)
    }
}
