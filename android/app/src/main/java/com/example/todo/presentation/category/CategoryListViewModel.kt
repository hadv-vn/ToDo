package com.example.todo.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.category.DeleteCategoryUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.presentation.common.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** FR-22 (docs/Task_Category_ToDo.md mục 5.3) — cùng khuôn [com.example.todo.presentation.tasklist.TaskListViewModel]. */
@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = getCategoriesUseCase()
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, categories = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = UiError(result.message ?: "Có lỗi xảy ra, vui lòng thử lại"))
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError("Không có kết nối mạng, vui lòng thử lại", isNetworkError = true)
                        )
                    }
                }
            }
        }
    }

    /** Chặn double-confirm — cùng pattern `onDeleteTaskConfirmed` (TaskListViewModel). */
    fun onDeleteCategoryConfirmed(categoryId: String) {
        if (categoryId in _uiState.value.pendingDeleteCategoryIds) return
        _uiState.update { it.copy(pendingDeleteCategoryIds = it.pendingDeleteCategoryIds + categoryId) }

        viewModelScope.launch {
            val result = deleteCategoryUseCase(categoryId)
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            categories = it.categories.filterNot { category -> category.id == categoryId },
                            pendingDeleteCategoryIds = it.pendingDeleteCategoryIds - categoryId
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            pendingDeleteCategoryIds = it.pendingDeleteCategoryIds - categoryId,
                            error = UiError(result.message ?: "Có lỗi xảy ra, vui lòng thử lại")
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            pendingDeleteCategoryIds = it.pendingDeleteCategoryIds - categoryId,
                            error = UiError("Không có kết nối mạng, vui lòng thử lại", isNetworkError = true)
                        )
                    }
                }
            }
        }
    }

    fun onErrorConsumed() {
        _uiState.update { it.copy(error = null) }
    }
}
