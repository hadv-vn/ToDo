package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

/**
 * `user` object trong response register/login — theo API_Contract_ToDo_Android.md mục 2.1/2.2.
 *
 * Lưu ý: `createdAt` là field CHÍNH THỨC theo API_Contract (khác giả định ở LLD mục 3.5 — LLD chỉ
 * có `id`, `email`; API_Contract mục 6 ghi rõ đã bổ sung `createdAt` so với LLD). DTO này theo
 * đúng API_Contract vì đây là nguồn chân lý. `createdAt` chưa cần dùng ở Domain (AuthUser không có
 * field này ở Phase 0) nên UserMapper không map field này — không mất thông tin nguy hiểm vì
 * createdAt chỉ hiển thị, không ảnh hưởng logic nghiệp vụ Auth.
 */
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val createdAt: String
)
