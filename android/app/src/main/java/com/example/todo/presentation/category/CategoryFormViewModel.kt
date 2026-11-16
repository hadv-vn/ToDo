package com.example.todo.presentation.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.category.CreateCategoryUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.domain.usecase.category.UpdateCategoryUseCase
import com.example.todo.presentation.common.UiError
import com.example.todo.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dùng chung cho CreateCategoryScreen (categoryId == null) và EditCategoryScreen — cùng pattern
 * [com.example.todo.presentation.taskform.TaskFormViewModel].
 *
 * Backend KHÔNG có endpoint lấy 1 category theo id (chỉ list/create/update/delete, xem
 * docs/Task_Category_ToDo.md mục 4.3) — Edit mode tải qua [GetCategoriesUseCase] rồi tự tìm đúng
 * `categoryId` trong danh sách, tránh phải thêm endpoint mới chỉ để phục vụ 1 màn hình Sửa.
 */
@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase
) : ViewModel() {

    private val categoryId: String? = savedStateHandle.get<String>(Routes.EDIT_CATEGORY_ARG_CATEGORY_ID)

    private val _uiState = MutableStateFlow(
        CategoryFormUiState(
            categoryId = categoryId,
            name = savedStateHandle.get<String>(KEY_NAME).orEmpty()
        )
    )
    val uiState: StateFlow<CategoryFormUiState> = _uiState.asStateFlow()

    init {
        if (categoryId != null && _uiState.value.name.isEmpty()) {
            loadCategoryForEdit(categoryId)
        }
    }

    private fun loadCategoryForEdit(id: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = getCategoriesUseCase()
            when (result) {
                is ApiResult.Success -> {
                    val category = result.data.find { it.id == id }
                    if (category == null) {
                        // Đã bị xóa ở nơi khác giữa lúc mở CategoryListScreen và mở màn Sửa.
                        _uiState.update { it.copy(isLoading = false, categoryDeletedRemotely = true) }
                    } else {
                        savedStateHandle[KEY_NAME] = category.name
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                name = category.name,
                                color = category.color,
                                icon = category.icon
                            )
                        }
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, submitError = UiError(result.message ?: "Không thể tải danh mục"))
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(isLoading = false, submitError = UiError("Không thể tải danh mục", isNetworkError = true))
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        savedStateHandle[KEY_NAME] = value
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onColorSelected(color: String) {
        _uiState.update { it.copy(color = color) }
    }

    fun onIconSelected(icon: String) {
        _uiState.update { it.copy(icon = icon) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (state.name.trim().isEmpty()) {
            _uiState.update { it.copy(nameError = "Tên danh mục không được để trống") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = if (categoryId == null) {
                createCategoryUseCase(state.name, state.color, state.icon)
            } else {
                updateCategoryUseCase(categoryId, state.name, state.color, state.icon)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, isSubmitSuccess = true) }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = UiError(result.message ?: "Đã có lỗi xảy ra, vui lòng thử lại")
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = UiError("Không có kết nối mạng, vui lòng thử lại", isNetworkError = true)
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val KEY_NAME = "name"
    }
}
