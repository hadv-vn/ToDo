package com.example.todo.presentation.taskform

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dueDateFieldFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * FR-20 (docs/Task_DueDate_ToDo.md mục 5.3/6.1) — dùng chung cho CreateTaskScreen/EditTaskScreen.
 * Dùng `DatePickerDialog` của Material3 có sẵn trong BOM hiện tại (cùng BOM đang cấp
 * `PullToRefreshBox`) — không cần thêm dependency mới.
 *
 * Quy đổi `LocalDate` <-> epoch millis qua UTC (KHÔNG qua timezone thiết bị): `DatePickerState`
 * làm việc với epoch millis UTC-based theo đúng tài liệu Compose, nên quy đổi qua múi giờ thiết bị
 * sẽ làm lệch ngày hiển thị gần nửa đêm ở một số timezone — giữ nhất quán UTC xuyên suốt tránh bug
 * lệch 1 ngày.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateField(
    dueDate: LocalDate?,
    onDueDateChange: (LocalDate?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Hạn chót: ", style = MaterialTheme.typography.bodyMedium)
        if (dueDate != null) {
            Text(dueDate.format(dueDateFieldFormatter))
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { onDueDateChange(null) }, enabled = enabled) {
                Text("Xóa hạn")
            }
        } else {
            TextButton(onClick = { showDialog = true }, enabled = enabled) {
                Text("+ Thêm hạn chót")
            }
        }
        if (dueDate != null) {
            TextButton(onClick = { showDialog = true }, enabled = enabled) {
                Text("Đổi")
            }
        }
    }

    if (showDialog) {
        val initialMillis = (dueDate ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDueDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDialog = false
                }) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
