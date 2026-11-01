package com.example.todo.data.remote.dto.task

import kotlinx.serialization.Serializable

/**
 * Theo API_Contract_ToDo_Android.md mục 3.1. `status` KHÔNG có field này — client không được set,
 * server tự mặc định "open".
 */
@Serializable
data class CreateTaskRequestDto(
    val title: String,
    val description: String? = null
)
