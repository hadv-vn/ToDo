package com.example.todo.presentation.category

import com.example.todo.domain.model.Category
import com.example.todo.presentation.common.UiError

/** FR-22 (docs/Task_Category_ToDo.md mục 5.3). */
data class CategoryListUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: UiError? = null,
    val pendingDeleteCategoryIds: Set<String> = emptySet()
)
