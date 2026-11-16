package com.example.todo.data.mapper

import com.example.todo.data.remote.dto.category.CategoryDto
import com.example.todo.domain.model.Category

fun CategoryDto.toDomain(): Category = Category(id = id, name = name, color = color, icon = icon)
