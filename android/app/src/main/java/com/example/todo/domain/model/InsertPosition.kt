package com.example.todo.domain.model

/**
 * Vị trí chèn task mới tạo trong danh sách — FR-26 (docs/Task_Reorder_ToDo.md). Mặc định [TOP]
 * (khớp DEFAULT_INSERT_POSITION ở backend). Mapping tới/từ chuỗi wire "top"/"bottom" thuộc tầng
 * Data (xem data/mapper/TaskMapper.kt), cùng nguyên tắc đã áp dụng cho TaskStatus.
 */
enum class InsertPosition {
    TOP,
    BOTTOM
}
