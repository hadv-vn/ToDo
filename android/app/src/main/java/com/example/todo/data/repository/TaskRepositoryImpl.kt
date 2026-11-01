package com.example.todo.data.repository

import com.example.todo.data.mapper.toDomain
import com.example.todo.data.mapper.toWireValue
import com.example.todo.data.remote.api.TaskApiService
import com.example.todo.data.remote.dto.task.CreateTaskRequestDto
import com.example.todo.data.remote.dto.task.UpdateTaskDetailsRequestDto
import com.example.todo.data.remote.dto.task.UpdateTaskStatusRequestDto
import com.example.todo.data.remote.safeApiCall
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Implementation thật của [TaskRepository] — theo LLD mục 3.5, tái sử dụng [safeApiCall] (Phase 1,
 * KHÔNG viết lại).
 *
 * editTask (sửa title/description) và toggleTaskStatus (đổi status) tách thành 2 method Domain
 * riêng nhưng CÙNG gọi [TaskApiService.updateTask] (PUT /tasks/:id) ở tầng HTTP — quyết định LLD
 * mục 3.4/9.3.
 */
class TaskRepositoryImpl @Inject constructor(
    private val taskApiService: TaskApiService
) : TaskRepository {

    override suspend fun getTasks(filter: TaskStatus?): ApiResult<List<Task>> {
        return safeApiCall { taskApiService.getTasks(filter?.toWireValue()) }
            .mapSuccess { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getTaskById(id: String): ApiResult<Task> {
        return safeApiCall { taskApiService.getTaskById(id) }.mapSuccess { it.toDomain() }
    }

    override suspend fun createTask(title: String, description: String?): ApiResult<Task> {
        return safeApiCall {
            taskApiService.createTask(CreateTaskRequestDto(title = title, description = description))
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun editTask(id: String, title: String, description: String?): ApiResult<Task> {
        return safeApiCall {
            taskApiService.updateTaskDetails(id, UpdateTaskDetailsRequestDto(title = title, description = description))
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus): ApiResult<Task> {
        return safeApiCall {
            taskApiService.updateTaskStatus(id, UpdateTaskStatusRequestDto(status = newStatus.toWireValue()))
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun deleteTask(id: String): ApiResult<Unit> {
        // taskApiService.deleteTask trả Response<Void> (xem TaskApiService — lý do dùng Void thay
        // Unit cho 204 No Content thực sự rỗng). Bỏ qua giá trị null, chỉ cần biết có thành công
        // hay không.
        return safeApiCall { taskApiService.deleteTask(id) }.mapSuccess { }
    }
}

private inline fun <T, R> ApiResult<T>.mapSuccess(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
    is ApiResult.NetworkError -> this
}
