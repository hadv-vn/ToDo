package com.example.todo.data.remote.dto.task

import kotlinx.serialization.Serializable

/**
 * Theo API_Contract_ToDo_Android.md mục 3.1. `status` KHÔNG có field này — client không được set,
 * server tự mặc định "open". `insertPosition` — FR-26 (docs/Task_Reorder_ToDo.md mục 4.3), không
 * gửi -> server tự mặc định 'top'.
 */
@Serializable
data class CreateTaskRequestDto(
    val title: String,
    val description: String? = null,
    val insertPosition: String? = null,
    val categoryId: String? = null,
    val dueDate: String? = null,
    val priority: String
)
