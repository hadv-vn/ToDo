package com.example.todo.domain.usecase.category

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.repository.CategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: String, name: String, color: String, icon: String): ApiResult<Category> {
        if (name.trim().isEmpty()) {
            return ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Tên danh mục không được để trống")
        }
        return categoryRepository.updateCategory(id, name.trim(), color, icon)
    }
}
