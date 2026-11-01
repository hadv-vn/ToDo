package com.example.todo.presentation.common

/**
 * Theo LLD mục 4.2 (nguyên văn) — dùng chung cho `TaskListUiState.error` và
 * `TaskFormUiState.submitError`. `isNetworkError` cho phép UI phân biệt lỗi mạng (có thể gợi ý
 * "Thử lại") với lỗi nghiệp vụ khác nếu cần, không bắt buộc dùng ngay ở Phase 2.
 */
data class UiError(val message: String, val isNetworkError: Boolean = false)
