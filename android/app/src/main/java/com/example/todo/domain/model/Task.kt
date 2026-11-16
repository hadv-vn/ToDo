package com.example.todo.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Domain Entity Task — theo LLD mục 3.2. Kotlin thuần, KHÔNG import android.*.
 * Dùng java.time.Instant (JDK thuần, có sẵn native từ API 26 = minSdk của app này,
 * không cần desugaring) thay cho kotlin.time.Instant để không phải thêm dependency
 * kotlinx-datetime chưa được HLD/LLD chỉ định.
 */
data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val categoryId: String? = null,
    // docs/Task_DueDate_ToDo.md mục 5.1 — chỉ ngày, không giờ; dùng LocalDate (JDK thuần, cùng
    // quyết định Phase 0 dùng java.time.* thay kotlinx-datetime, xem comment đầu file).
    val dueDate: LocalDate? = null,
    // docs/Task_Priority_ToDo.md mục 5.1 — luôn có giá trị, mặc định MEDIUM (không như
    // categoryId/dueDate không có khái niệm "không chọn gì").
    val priority: TaskPriority = TaskPriority.MEDIUM
)
