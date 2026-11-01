package com.example.todo.presentation.tasklist

import com.example.todo.domain.model.TaskStatus

/**
 * Theo LLD mục 4.2 (nguyên văn). `descriptionPreview` đã truncate 1 dòng ở Mapper tầng
 * Presentation (xem `Task.toUiModel()` trong TaskListViewModel.kt). `isTogglePending` — cờ
 * per-item chống double-tap (LLD mục 4.4a).
 */
data class TaskUiModel(
    val id: String,
    val title: String,
    val descriptionPreview: String?,
    val status: TaskStatus,
    val isTogglePending: Boolean = false
)
