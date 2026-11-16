package com.example.todo.domain.repository

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskPriority
import com.example.todo.domain.model.TaskStatus
import java.time.LocalDate

/**
 * Repository interface cho Task — theo LLD mục 3.4 (đã bổ sung getTaskById theo
 * docs/API_Contract_ToDo_Android.md mục 0.1 / LLD mục 5 — dùng để load dữ liệu cho EditTaskScreen,
 * thay thế giải pháp SelectedTaskHolder in-memory đã bị loại bỏ).
 *
 * Quyết định LLD: editTask (sửa title/description) và toggleTaskStatus (đổi status) tách thành
 * 2 method riêng dù cả hai cùng gọi PUT /tasks/:id ở tầng HTTP — tránh dùng tham số null làm
 * "sentinel nghĩa là không đổi".
 *
 * Implementation thật (TaskRepositoryImpl) thuộc Phase 2, KHÔNG implement ở Phase 0.
 */
interface TaskRepository {
    suspend fun getTasks(filter: TaskStatus? = null): ApiResult<List<Task>>

    // Dùng để load dữ liệu cho EditTaskScreen.
    suspend fun getTaskById(id: String): ApiResult<Task>

    suspend fun createTask(
        title: String,
        description: String?,
        insertPosition: InsertPosition = InsertPosition.TOP,
        categoryId: String? = null,
        dueDate: LocalDate? = null,
        priority: TaskPriority = TaskPriority.MEDIUM
    ): ApiResult<Task>

    /**
     * [categoryId] — docs/Task_Category_ToDo.md mục 5.1. `null` tường minh = bỏ danh mục.
     * [dueDate] — docs/Task_DueDate_ToDo.md mục 5.1. `null` tường minh = xóa hạn.
     */
    suspend fun editTask(
        id: String,
        title: String,
        description: String?,
        categoryId: String? = null,
        dueDate: LocalDate? = null,
        priority: TaskPriority = TaskPriority.MEDIUM
    ): ApiResult<Task>
    suspend fun toggleTaskStatus(id: String, newStatus: TaskStatus): ApiResult<Task>

    /** FR-25 (docs/Task_Reorder_ToDo.md) — `null` ở 1 trong 2 tham chiếu nghĩa là kéo vào đầu/cuối. */
    suspend fun reorderTask(id: String, afterTaskId: String?, beforeTaskId: String?): ApiResult<Task>
    suspend fun deleteTask(id: String): ApiResult<Unit>
}
