package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskFilter
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — map TaskFilter.ALL -> null khi gọi Repository (API_Contract mục 1.7: vắng
 * mặt query `status` nghĩa là "all", KHÔNG gửi `status=all` thẳng ra).
 */
class GetTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(filter: TaskFilter): ApiResult<List<Task>> {
        val statusFilter = when (filter) {
            TaskFilter.ALL -> null
            TaskFilter.OPEN -> TaskStatus.OPEN
            TaskFilter.DONE -> TaskStatus.DONE
        }
        return taskRepository.getTasks(statusFilter)
    }
}
