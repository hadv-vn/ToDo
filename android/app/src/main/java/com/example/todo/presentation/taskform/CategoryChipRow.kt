package com.example.todo.presentation.taskform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todo.domain.model.Category

/**
 * FR-23 (docs/Task_Category_ToDo.md mục 5.3/6.3) — hàng chip chọn danh mục, dùng chung cho
 * CreateTaskScreen/EditTaskScreen. Tái dùng `FilterChip` đã dùng cho All/Open/Done ở
 * TaskListScreen.kt (nguyên tắc #9 — quy ước thay cho cấu hình, không nhập component mới).
 *
 * `LazyRow` (không phải `Row` thường) vì số danh mục không cố định — người dùng có thể tạo nhiều
 * hơn số chip vừa 1 màn hình ngang.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChipRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onNavigateToCategoryList: (() -> Unit)? = null
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                enabled = enabled,
                label = { Text("Không danh mục") }
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                enabled = enabled,
                label = { Text("${category.icon} ${category.name}") }
            )
        }
        // AC2 FR-23 (docs/Task_Category_ToDo.md) — chưa có danh mục nào -> dẫn sang màn Quản lý.
        if (categories.isEmpty() && onNavigateToCategoryList != null) {
            item {
                FilterChip(
                    selected = false,
                    onClick = onNavigateToCategoryList,
                    enabled = enabled,
                    label = { Text("+ Tạo danh mục") }
                )
            }
        }
    }
}
