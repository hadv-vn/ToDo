package com.example.todo.data.remote.dto.category

import kotlinx.serialization.Serializable

/** Theo docs/Task_Category_ToDo.md mục 4 — KHÔNG có `userId` (ownership enforce ở backend). */
@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val createdAt: String
)

@Serializable
data class CategoryEnvelopeDto(val data: CategoryDto)

@Serializable
data class CategoryListEnvelopeDto(val data: List<CategoryDto>)
