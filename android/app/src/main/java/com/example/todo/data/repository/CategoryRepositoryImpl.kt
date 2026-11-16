package com.example.todo.data.repository

import com.example.todo.data.mapper.toDomain
import com.example.todo.data.remote.api.CategoryApiService
import com.example.todo.data.remote.dto.category.CreateCategoryRequestDto
import com.example.todo.data.remote.dto.category.UpdateCategoryRequestDto
import com.example.todo.data.remote.safeApiCall
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.repository.CategoryRepository
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryApiService: CategoryApiService
) : CategoryRepository {

    override suspend fun getCategories(): ApiResult<List<Category>> {
        return safeApiCall { categoryApiService.getCategories() }
            .mapSuccess { envelope -> envelope.data.map { it.toDomain() } }
    }

    override suspend fun createCategory(name: String, color: String?, icon: String?): ApiResult<Category> {
        return safeApiCall {
            categoryApiService.createCategory(CreateCategoryRequestDto(name = name, color = color, icon = icon))
        }.mapSuccess { it.data.toDomain() }
    }

    override suspend fun updateCategory(id: String, name: String, color: String, icon: String): ApiResult<Category> {
        return safeApiCall {
            categoryApiService.updateCategory(id, UpdateCategoryRequestDto(name = name, color = color, icon = icon))
        }.mapSuccess { it.data.toDomain() }
    }

    override suspend fun deleteCategory(id: String): ApiResult<Unit> {
        return safeApiCall { categoryApiService.deleteCategory(id) }.mapSuccess { }
    }
}

private inline fun <T, R> ApiResult<T>.mapSuccess(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
    is ApiResult.NetworkError -> this
}
