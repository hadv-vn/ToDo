package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — gọi TaskRepository.editTask (chỉ sửa title/description, KHÔNG đổi status) —
 * tách riêng khỏi ToggleTaskStatusUseCase theo quyết định LLD mục 3.4/9.3 dù cả hai cùng gọi
 * PUT /tasks/:id ở tầng HTTP.
 */
class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: String, title: String, description: String?): ApiResult<Task> {
        if (title.trim().isEmpty()) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Tiêu đề không được để trống")
        }
        return taskRepository.editTask(id, title.trim(), description?.takeIf { it.isNotBlank() })
    }
}
