package com.example.todo.data.remote.dto.task

import kotlinx.serialization.Serializable

/**
 * Body cho [com.example.todo.data.remote.api.TaskApiService.updateTaskStatus] (dùng bởi
 * `toggleTaskStatus` — chỉ đổi status, KHÔNG đụng title/description).
 *
 * Tách riêng khỏi [UpdateTaskDetailsRequestDto] vì cùng lý do nêu ở file đó: `status` không có
 * giá trị default nên luôn được serialize, và việc tách DTO đảm bảo request toggle KHÔNG BAO GIỜ
 * vô tình gửi kèm `title`/`description` (dù là `null` hay giá trị thật) — tránh mọi khả năng ghi
 * đè nhầm 2 field này khi chỉ muốn đổi trạng thái.
 */
@Serializable
data class UpdateTaskStatusRequestDto(
    val status: String
)
