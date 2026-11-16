package com.example.todo.data.remote.dto.category

import kotlinx.serialization.Serializable

@Serializable
data class CreateCategoryRequestDto(
    val name: String,
    val color: String? = null,
    val icon: String? = null
)
