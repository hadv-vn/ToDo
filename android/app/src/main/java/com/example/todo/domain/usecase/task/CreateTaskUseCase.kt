package com.example.todo.domain.usecase.task

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskPriority
import com.example.todo.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — validate title.trim().isNotEmpty() mang tính defensive (ViewModel validate
 * lần đầu để hiển thị lỗi inline tức thời, đây là lớp bảo vệ invariant nghiệp vụ thứ hai, không
 * phải trùng lặp vô ích — xem LLD mục 4.1 đoạn cuối). description rỗng/chỉ-khoảng-trắng -> gửi
 * null (server lưu null, tránh lưu chuỗi rỗng vô nghĩa).
 *
 * [insertPosition] — FR-26 (docs/Task_Reorder_ToDo.md): vị trí chèn task mới, đọc từ cài đặt
 * trên thiết bị qua [com.example.todo.domain.usecase.preferences.GetInsertPositionPreferenceUseCase]
 * ở ViewModel, mặc định [InsertPosition.TOP] nếu caller không truyền (ví dụ test cũ gọi 2 tham số).
 */
class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        insertPosition: InsertPosition = InsertPosition.TOP,
        categoryId: String? = null,
        dueDate: LocalDate? = null,
        priority: TaskPriority = TaskPriority.MEDIUM
    ): ApiResult<Task> {
        if (title.trim().isEmpty()) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Tiêu đề không được để trống")
        }
        return taskRepository.createTask(
            title.trim(),
            description?.takeIf { it.isNotBlank() },
            insertPosition,
            categoryId,
            dueDate,
            priority
        )
    }
}
