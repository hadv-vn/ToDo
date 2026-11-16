package com.example.todo.domain.repository

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category

/**
 * docs/Task_Category_ToDo.md mục 5.1 — cùng pattern [TaskRepository]. [updateCategory] nhận đủ 3
 * field KHÔNG optional: form Sửa danh mục luôn tải đủ giá trị hiện tại trước khi cho sửa, nên
 * không có khái niệm "chỉ đổi 1 field, giữ field khác" ở phía Android (khác `editTask` của Task
 * vẫn cần optional cho `description`/`categoryId` vì có ngữ nghĩa "xóa"/"bỏ danh mục").
 */
interface CategoryRepository {
    suspend fun getCategories(): ApiResult<List<Category>>
    suspend fun createCategory(name: String, color: String?, icon: String?): ApiResult<Category>
    suspend fun updateCategory(id: String, name: String, color: String, icon: String): ApiResult<Category>
    suspend fun deleteCategory(id: String): ApiResult<Unit>
}
