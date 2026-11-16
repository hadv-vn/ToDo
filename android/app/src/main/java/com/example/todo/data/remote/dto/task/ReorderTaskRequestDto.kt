package com.example.todo.data.remote.dto.task

import kotlinx.serialization.Serializable

/**
 * Body cho `PATCH /api/v1/tasks/:id/reorder` — docs/Task_Reorder_ToDo.md mục 4.2. Không có giá
 * trị default: luôn serialize cả 2 field tường minh (kể cả `null`) — cùng lý do đã ghi chú ở
 * [UpdateTaskDetailsRequestDto] (kotlinx.serialization `encodeDefaults = false` sẽ lược field
 * bằng default khỏi JSON; ở đây `null` tường minh có ý nghĩa "kéo vào đầu/cuối danh sách").
 */
@Serializable
data class ReorderTaskRequestDto(
    val afterTaskId: String?,
    val beforeTaskId: String?
)
