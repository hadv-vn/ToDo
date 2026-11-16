package com.example.todo.domain.usecase.category

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: String): ApiResult<Unit> = categoryRepository.deleteCategory(id)
}
