package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * FR-25 (docs/Task_Reorder_ToDo.md mục 3) — không có rule nghiệp vụ cần validate ở tầng này
 * (ownership của afterTaskId/beforeTaskId đã enforce ở backend, xem task.service.ts), chỉ gọi
 * thẳng Repository — cùng pattern [DeleteTaskUseCase].
 */
class ReorderTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: String, afterTaskId: String?, beforeTaskId: String?): ApiResult<Task> =
        taskRepository.reorderTask(id, afterTaskId, beforeTaskId)
}
