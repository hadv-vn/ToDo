package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — không cần validate thêm, chỉ gọi thẳng Repository.
 */
class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: String): ApiResult<Unit> = taskRepository.deleteTask(id)
}
