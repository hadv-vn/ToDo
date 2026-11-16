package com.example.todo.presentation.taskform

import com.example.todo.domain.model.Category
import com.example.todo.domain.model.TaskPriority
import com.example.todo.presentation.common.UiError
import java.time.LocalDate

/**
 * Theo LLD mục 4.2 (struct nguyên văn) + mục 5 (bổ sung `isLoading` — pseudocode "Luồng tải dữ
 * liệu EditTaskScreen" ở mục 5 dùng `updateState { it.copy(isLoading = true) }` khi fetch dữ liệu
 * Edit, trường này cần có trong struct để pseudocode đó hợp lệ).
 *
 * `taskId == null` -> Create mode; `taskId != null` -> Edit mode (đọc từ SavedStateHandle, xem
 * TaskFormViewModel).
 */
data class TaskFormUiState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val titleError: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: UiError? = null,
    val taskDeletedRemotely: Boolean = false,
    val isSubmitSuccess: Boolean = false,
    // FR-23 (docs/Task_Category_ToDo.md) — `null` = "Không danh mục" (mặc định).
    val categoryId: String? = null,
    val availableCategories: List<Category> = emptyList(),
    // FR-20 (docs/Task_DueDate_ToDo.md) — `null` = "Chưa đặt hạn" (mặc định).
    val dueDate: LocalDate? = null,
    // FR-18 (docs/Task_Priority_ToDo.md) — mặc định Trung bình.
    val priority: TaskPriority = TaskPriority.MEDIUM
)
