package com.example.todo.domain.common

/**
 * Mã lỗi đã chuẩn hóa từ HTTP status code (hoặc lỗi parse/không lường trước) — theo LLD mục 3.3.
 * Dùng xuyên suốt Repository -> UseCase -> ViewModel, không rò rỉ chi tiết HTTP status thô lên trên.
 */
enum class ApiErrorCode {
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    SERVER_ERROR,
    UNKNOWN
}
