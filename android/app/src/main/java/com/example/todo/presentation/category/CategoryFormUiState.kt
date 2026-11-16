package com.example.todo.presentation.category

import com.example.todo.domain.model.CategoryAppearance
import com.example.todo.presentation.common.UiError

/** `categoryId == null` -> Create mode; khác null -> Edit mode — cùng pattern `TaskFormUiState`. */
data class CategoryFormUiState(
    val categoryId: String? = null,
    val name: String = "",
    val color: String = CategoryAppearance.DEFAULT_COLOR,
    val icon: String = CategoryAppearance.DEFAULT_ICON,
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: UiError? = null,
    val categoryDeletedRemotely: Boolean = false,
    val isSubmitSuccess: Boolean = false
)
