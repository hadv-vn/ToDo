package com.example.todo.data.remote.dto.task

import kotlinx.serialization.Serializable

/**
 * Body cho [com.example.todo.data.remote.api.TaskApiService.updateTaskDetails] (dùng bởi
 * `editTask` — sửa title/description, KHÔNG đổi status).
 *
 * CHÚ Ý QUAN TRỌNG — lý do tách DTO riêng thay vì dùng chung 1 DTO với toggleTaskStatus (như
 * pseudocode gốc LLD mục 3.5 gợi ý "`UpdateTaskRequestDto` với 3 field optional dùng chung"):
 * kotlinx.serialization mặc định `encodeDefaults = false`, nghĩa là field có giá trị ĐÚNG BẰNG
 * default khai báo (`= null`) sẽ bị LƯỢC BỎ KHỎI JSON OUTPUT, không chỉ encode thành `null`. Nếu
 * dùng chung 1 DTO 3-field-optional, khi người dùng xóa trắng mô tả rồi Lưu, `description = null`
 * sẽ bị lược bỏ khỏi request — backend coi như "không gửi field" (giữ nguyên mô tả cũ) thay vì
 * "xóa mô tả" như API_Contract mục 3.4 yêu cầu (`"description": null` tường minh để xóa).
 *
 * Ở đây `title`/`description` KHÔNG có giá trị default — bắt buộc phải truyền tường minh khi khởi
 * tạo, nên kotlinx.serialization LUÔN serialize cả 2 field bất kể giá trị gì (kể cả `null`),
 * không phụ thuộc `encodeDefaults`. `editTask` luôn có `title` thật (không bao giờ null) và
 * `description` có thể là chuỗi thật hoặc `null` (xóa mô tả) — cả 2 trường hợp đều được gửi đúng.
 */
@Serializable
data class UpdateTaskDetailsRequestDto(
    val title: String,
    val description: String?
)
