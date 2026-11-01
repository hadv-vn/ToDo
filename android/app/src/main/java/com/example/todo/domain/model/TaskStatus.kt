package com.example.todo.domain.model

/**
 * Trạng thái nghiệp vụ của một Task — Kotlin thuần, không phụ thuộc android.* hay JSON wire format.
 * Việc map từ/đến chuỗi JSON "open"/"done" thuộc tầng Data (xem data/mapper/TaskMapper.kt).
 */
enum class TaskStatus {
    OPEN,
    DONE
}
