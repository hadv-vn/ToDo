package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — tự tính newStatus = opposite(currentStatus) rồi gọi
 * TaskRepository.toggleTaskStatus. ViewModel (mục 4.4a) chịu trách nhiệm double-tap
 * protection/optimistic update/rollback; UseCase này chỉ thuần tính toán + gọi Repository.
 */
class ToggleTaskStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: String, currentStatus: TaskStatus): ApiResult<Task> {
        val newStatus = if (currentStatus == TaskStatus.OPEN) TaskStatus.DONE else TaskStatus.OPEN
        return taskRepository.toggleTaskStatus(id, newStatus)
    }
}
