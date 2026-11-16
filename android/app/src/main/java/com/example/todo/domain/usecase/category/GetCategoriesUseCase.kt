package com.example.todo.domain.usecase.category

import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.repository.CategoryRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(): ApiResult<List<Category>> = categoryRepository.getCategories()
}
