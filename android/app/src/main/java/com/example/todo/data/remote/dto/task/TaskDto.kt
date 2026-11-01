package com.example.todo.data.remote.dto.task

import kotlinx.serialization.Serializable

/**
 * Task object schema dùng chung cho response thành công của mọi endpoint Task — theo
 * API_Contract_ToDo_Android.md mục 3. KHÔNG có `ownerId` (API_Contract mục 3: "KHÔNG expose
 * ownerId" — ownership enforce nội bộ ở backend qua req.userId).
 */
@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
