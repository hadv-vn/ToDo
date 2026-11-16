package com.example.todo.domain.model

/**
 * Danh mục task do người dùng tự tạo — docs/Task_Category_ToDo.md. `color` là chuỗi hex
 * (`"#1E88E5"`), `icon` là 1 emoji — cả 2 từ bộ cố định do backend validate, Android chỉ hiển thị
 * trực tiếp, không parse/suy luận gì thêm.
 */
data class Category(
    val id: String,
    val name: String,
    val color: String,
    val icon: String
)
