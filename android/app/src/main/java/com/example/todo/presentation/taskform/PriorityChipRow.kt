package com.example.todo.presentation.taskform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todo.domain.model.TaskPriority

/**
 * FR-18 (docs/Task_Priority_ToDo.md mục 5.3/6.1) — hàng 3 chip Cao/Trung bình/Thấp, dùng chung cho
 * CreateTaskScreen/EditTaskScreen. Tái dùng `FilterChip` (nguyên tắc #9 — quy ước thay cho cấu
 * hình, không nhập component mới). Luôn đúng 3 chip cố định -> `Row` thường (khác `CategoryChipRow`
 * phải dùng `LazyRow` vì số danh mục không cố định).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityChipRow(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedPriority == TaskPriority.HIGH,
            onClick = { onPrioritySelected(TaskPriority.HIGH) },
            enabled = enabled,
            label = { Text("Cao") }
        )
        FilterChip(
            selected = selectedPriority == TaskPriority.MEDIUM,
            onClick = { onPrioritySelected(TaskPriority.MEDIUM) },
            enabled = enabled,
            label = { Text("Trung bình") }
        )
        FilterChip(
            selected = selectedPriority == TaskPriority.LOW,
            onClick = { onPrioritySelected(TaskPriority.LOW) },
            enabled = enabled,
            label = { Text("Thấp") }
        )
    }
}
