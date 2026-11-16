package com.example.todo.domain.usecase.category

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.repository.CategoryRepository
import javax.inject.Inject

/** Validate tên không rỗng — cùng tinh thần defensive của [com.example.todo.domain.usecase.task.CreateTaskUseCase]. */
class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(name: String, color: String?, icon: String?): ApiResult<Category> {
        if (name.trim().isEmpty()) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Tên danh mục không được để trống")
        }
        return categoryRepository.createCategory(name.trim(), color, icon)
    }
}
