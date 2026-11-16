package com.example.todo.presentation.taskform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.TaskPriority
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.domain.usecase.preferences.GetInsertPositionPreferenceUseCase
import com.example.todo.domain.usecase.task.CreateTaskUseCase
import com.example.todo.domain.usecase.task.GetTaskByIdUseCase
import com.example.todo.domain.usecase.task.UpdateTaskUseCase
import com.example.todo.presentation.common.UiError
import com.example.todo.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Dùng chung cho CreateTaskScreen (taskId == null) và EditTaskScreen (taskId != null) — theo LLD
 * mục 4.2/4.4(b)/mục 5.
 *
 * - `taskId` đọc từ [SavedStateHandle] — Hilt Navigation Compose tự gắn nav argument "taskId" của
 *   route `edit_task/{taskId}` vào SavedStateHandle khi dùng `hiltViewModel()` (route `create_task`
 *   không có arg này -> null -> Create mode), giống cách [com.example.todo.presentation.auth.ResetPasswordViewModel]
 *   đọc "email".
 * - Giữ dữ liệu form khi xoay màn hình/process death bằng SavedStateHandle (LLD mục 4.4b):
 *   title/description được ghi đồng thời vào SavedStateHandle mỗi lần onXxxChange.
 * - Edit mode: tự fetch dữ liệu qua [GetTaskByIdUseCase] (GET /tasks/:id) khi init, KHÔNG dùng
 *   SelectedTaskHolder (đã loại bỏ khỏi thiết kế — LLD mục 5/10.3). Chỉ fetch nếu chưa có data
 *   (tránh fetch lại sau rotation khi ViewModel vẫn còn sống).
 */
@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getInsertPositionPreferenceUseCase: GetInsertPositionPreferenceUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val taskId: String? = savedStateHandle.get<String>(Routes.EDIT_TASK_ARG_TASK_ID)

    private val _uiState = MutableStateFlow(
        TaskFormUiState(
            taskId = taskId,
            title = savedStateHandle.get<String>(KEY_TITLE).orEmpty(),
            description = savedStateHandle.get<String>(KEY_DESCRIPTION).orEmpty(),
            categoryId = savedStateHandle.get<String>(KEY_CATEGORY_ID),
            dueDate = savedStateHandle.get<Long>(KEY_DUE_DATE)?.let { LocalDate.ofEpochDay(it) },
            priority = savedStateHandle.get<String>(KEY_PRIORITY)?.let { TaskPriority.valueOf(it) }
                ?: TaskPriority.MEDIUM
        )
    )
    val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

    init {
        if (taskId != null && _uiState.value.title.isEmpty()) {
            loadTaskForEdit(taskId)
        }
        loadCategories()
    }

    /** FR-23 (docs/Task_Category_ToDo.md mục 5.3) — tải 1 lần khi mở form, dùng cho hàng FilterChip. */
    private fun loadCategories() {
        viewModelScope.launch {
            val result = getCategoriesUseCase()
            if (result is ApiResult.Success) {
                _uiState.update { it.copy(availableCategories = result.data) }
            }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        savedStateHandle[KEY_CATEGORY_ID] = categoryId
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onDueDateChange(value: LocalDate?) {
        savedStateHandle[KEY_DUE_DATE] = value?.toEpochDay()
        _uiState.update { it.copy(dueDate = value) }
    }

    fun onPriorityChange(value: TaskPriority) {
        savedStateHandle[KEY_PRIORITY] = value.name
        _uiState.update { it.copy(priority = value) }
    }

    private fun loadTaskForEdit(id: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = getTaskByIdUseCase(id)
            when (result) {
                is ApiResult.Success -> {
                    val task = result.data
                    savedStateHandle[KEY_TITLE] = task.title
                    savedStateHandle[KEY_DESCRIPTION] = task.description.orEmpty()
                    savedStateHandle[KEY_CATEGORY_ID] = task.categoryId
                    savedStateHandle[KEY_DUE_DATE] = task.dueDate?.toEpochDay()
                    savedStateHandle[KEY_PRIORITY] = task.priority.name
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = task.title,
                            description = task.description ?: "",
                            categoryId = task.categoryId,
                            dueDate = task.dueDate,
                            priority = task.priority
                        )
                    }
                }
                is ApiResult.Error -> {
                    if (result.code == ApiErrorCode.NOT_FOUND) {
                        _uiState.update { it.copy(isLoading = false, taskDeletedRemotely = true) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                submitError = UiError(result.message ?: "Không thể tải dữ liệu task")
                            )
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            submitError = UiError("Không thể tải dữ liệu task", isNetworkError = true)
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        savedStateHandle[KEY_TITLE] = value
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun onDescriptionChange(value: String) {
        savedStateHandle[KEY_DESCRIPTION] = value
        _uiState.update { it.copy(description = value) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        if (state.title.trim().isEmpty()) {
            _uiState.update { it.copy(titleError = "Tiêu đề không được để trống") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val description = state.description.takeIf { it.isNotBlank() }
            val result = if (taskId == null) {
                // FR-26 (docs/Task_Reorder_ToDo.md) — đọc cài đặt vị trí thêm mới NGAY LÚC submit
                // (không đọc ở init) để luôn dùng giá trị mới nhất nếu người dùng vừa đổi cài đặt.
                createTaskUseCase(
                    state.title,
                    description,
                    getInsertPositionPreferenceUseCase(),
                    state.categoryId,
                    state.dueDate,
                    state.priority
                )
            } else {
                updateTaskUseCase(
                    taskId,
                    state.title,
                    description,
                    state.categoryId,
                    state.dueDate,
                    state.priority
                )
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, isSubmitSuccess = true) }
                }
                is ApiResult.Error -> {
                    if (taskId != null && result.code == ApiErrorCode.NOT_FOUND) {
                        // Edit gặp 404 -> task đã bị xóa ở nơi khác (LLD mục 4.4d).
                        _uiState.update { it.copy(isSubmitting = false, taskDeletedRemotely = true) }
                    } else {
                        val message = when (result.code) {
                            ApiErrorCode.BAD_REQUEST -> result.message ?: "Dữ liệu không hợp lệ"
                            else -> result.message ?: "Đã có lỗi xảy ra, vui lòng thử lại"
                        }
                        _uiState.update { it.copy(isSubmitting = false, submitError = UiError(message)) }
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
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_CATEGORY_ID = "categoryId"
        const val KEY_DUE_DATE = "dueDate"
        const val KEY_PRIORITY = "priority"
    }
}
