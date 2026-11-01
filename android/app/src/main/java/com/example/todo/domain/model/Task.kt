package com.example.todo.domain.model

import java.time.Instant

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
    val updatedAt: Instant
)
