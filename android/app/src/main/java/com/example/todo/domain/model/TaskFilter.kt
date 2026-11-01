package com.example.todo.domain.model

/**
 * Theo LLD mục 4.2 (nguyên văn liệt kê cùng TaskListUiState ở Presentation), nhưng đặt ở
 * domain/model (Kotlin thuần, không phụ thuộc Compose/Android) vì GetTasksUseCase (Domain, LLD
 * mục 4.1) nhận trực tiếp tham số kiểu TaskFilter — nếu để ở package `presentation.tasklist` thì
 * Domain sẽ phải import ngược lên Presentation, vi phạm hướng phụ thuộc Clean Architecture (LLD
 * mục 3.1 "domain không import android.*" là một ca cụ thể của nguyên tắc tổng quát này).
 * TaskListUiState (Presentation) import lại type này từ domain, không định nghĩa riêng.
 */
enum class TaskFilter { ALL, OPEN, DONE }
