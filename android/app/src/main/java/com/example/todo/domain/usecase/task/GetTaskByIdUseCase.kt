package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Dùng để load dữ liệu cho EditTaskScreen qua GET /tasks/:id (API_Contract_ToDo_Android.md mục
 * 3.3) — thay thế hoàn toàn giải pháp SelectedTaskHolder in-memory đã bị loại bỏ khỏi thiết kế
 * (LLD mục 5/10.3).
 */
class GetTaskByIdUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: String): ApiResult<Task> = taskRepository.getTaskById(id)
}
