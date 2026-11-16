package com.example.todo.presentation.tasklist

import com.example.todo.domain.model.TaskPriority
import com.example.todo.domain.model.TaskStatus
import java.time.LocalDate

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
    val isTogglePending: Boolean = false,
    val categoryId: String? = null,
    val dueDate: LocalDate? = null,
    // Tính sẵn ở mapper (Presentation), KHÔNG tính lại trong Composable — docs/Task_DueDate_ToDo.md
    // mục 5.4: `isOverdue` = dueDate != null && status == OPEN && dueDate < hôm nay.
    val isOverdue: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM
)
