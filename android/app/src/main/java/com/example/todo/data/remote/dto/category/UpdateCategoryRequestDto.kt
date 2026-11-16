package com.example.todo.data.remote.dto.category

import kotlinx.serialization.Serializable

/**
 * Luôn gửi đủ 3 field (không có default) — form Sửa danh mục luôn có đầy đủ giá trị hiện tại
 * (tải từ server khi mở màn hình), không có khái niệm "không đổi field này" ở UI, nên không cần
 * phân biệt "không gửi" vs "gửi giá trị mới" như `UpdateTaskDetailsRequestDto` phải xử lý cho
 * `description`.
 */
@Serializable
data class UpdateCategoryRequestDto(
    val name: String,
    val color: String,
    val icon: String
)
