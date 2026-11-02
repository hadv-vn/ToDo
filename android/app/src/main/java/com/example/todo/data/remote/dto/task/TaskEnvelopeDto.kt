package com.example.todo.data.remote.dto.task

import kotlinx.serialization.Serializable

/** auth-core bọc mọi response Task thành công trong `{ data: ... }`. */
@Serializable
data class TaskEnvelopeDto(val data: TaskDto)

@Serializable
data class TaskListEnvelopeDto(val data: List<TaskDto>)
