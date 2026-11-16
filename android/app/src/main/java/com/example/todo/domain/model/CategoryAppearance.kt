package com.example.todo.domain.model

/**
 * Bộ màu/icon cố định — mirror CATEGORY_COLOR_VALUES/CATEGORY_ICON_VALUES ở backend
 * (`category-appearance.constants.ts`). Android phải gửi đúng 1 trong các giá trị này (backend
 * validate `@IsIn`) — tránh phải thêm color-picker/icon-library mới (docs/Task_Category_ToDo.md
 * mục 4.1).
 */
object CategoryAppearance {
    val COLORS = listOf("#E53935", "#FB8C00", "#FDD835", "#43A047", "#1E88E5", "#5E35B1", "#8D6E63", "#757575")
    const val DEFAULT_COLOR = "#1E88E5"

    val ICONS = listOf("📁", "💼", "🏠", "🛒", "📚", "⭐", "❤️", "🎯")
    const val DEFAULT_ICON = "📁"
}
