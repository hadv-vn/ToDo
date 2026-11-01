package com.example.todo.presentation.tasklist

import com.example.todo.domain.model.TaskFilter
import com.example.todo.presentation.common.UiError

/**
 * Theo LLD mục 4.2 (nguyên văn struct). Phase 2: dựng đủ struct này, nhưng FilterChip UI đổi
 * `filter` thuộc Phase 3 — Phase 2 chỉ cần `loadTasks()` hoạt động đúng với filter mặc định
 * [TaskFilter.ALL] (CHƯA có `onFilterSelected` ở TaskListViewModel).
 */
data class TaskListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val tasks: List<TaskUiModel> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val error: UiError? = null,
    val emptyStateType: EmptyStateType? = null,
    // internal — dùng để tính emptyStateType, xem LLD mục 4.4(f). Phase 2 chưa đổi filter nên
    // trường này luôn phản ánh đúng kết quả load ALL gần nhất.
    val hasAnyTaskOverall: Boolean = false,
    // Phase 4 hardening: chặn double-confirm xóa (ví dụ bấm "Xóa" 2 lần liên tiếp trên dialog xác
    // nhận do lag UI) — cùng pattern với isTogglePending ở TaskUiModel, nhưng dùng Set riêng vì
    // task đang chờ xóa sẽ bị loại khỏi `tasks` ngay khi thành công nên không cần field trên
    // TaskUiModel.
    val pendingDeleteTaskIds: Set<String> = emptySet()
)

enum class EmptyStateType { NO_TASKS_AT_ALL, NO_TASKS_MATCH_FILTER }
