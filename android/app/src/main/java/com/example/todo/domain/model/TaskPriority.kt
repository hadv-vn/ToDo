package com.example.todo.domain.model

/**
 * Mức ưu tiên của task — docs/Task_Priority_ToDo.md. Kotlin thuần, không phụ thuộc Android.
 * Mapping tới/từ chuỗi wire "low"/"medium"/"high" thuộc tầng Data (xem
 * data/mapper/TaskMapper.kt), cùng nguyên tắc đã áp dụng cho TaskStatus/InsertPosition.
 */
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}
