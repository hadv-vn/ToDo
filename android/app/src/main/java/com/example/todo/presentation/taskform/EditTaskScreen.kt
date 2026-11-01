package com.example.todo.presentation.taskform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Theo LLD mục 4.2/mục 5 ("Luồng tải dữ liệu EditTaskScreen", đã cập nhật sau API Contract v1.0)
 * — implementation thật Phase 2 (thay thế placeholder Phase 0).
 *
 * Dùng chung [TaskFormViewModel] với CreateTaskScreen — ViewModel tự đọc `taskId` từ
 * `SavedStateHandle` (Hilt Navigation Compose tự gắn nav argument "taskId" của route
 * `edit_task/{taskId}`) và tự fetch dữ liệu qua `GetTaskByIdUseCase` (GET /tasks/:id). KHÔNG còn
 * dùng SelectedTaskHolder in-memory (đã loại bỏ khỏi thiết kế, xem LLD mục 5/10.3) — không cần
 * nhận `taskId` qua tham số Composable nữa.
 *
 * - `isLoading`: đang fetch dữ liệu task ban đầu.
 * - `taskDeletedRemotely`: 404 khi fetch HOẶC khi submit -> hiển thị thông báo rồi tự điều hướng
 *   quay lại TaskList qua [onSubmitSuccess] (tái dùng cùng callback với submit thành công vì cả
 *   2 trường hợp đều cần TaskList tự loadTasks() lại — task bị xóa thì cũng phải biến mất khỏi
 *   danh sách).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskFormViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onSubmitSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSubmitSuccess) {
        if (uiState.isSubmitSuccess) {
            onSubmitSuccess()
        }
    }

    LaunchedEffect(uiState.taskDeletedRemotely) {
        if (uiState.taskDeletedRemotely) {
            onSubmitSuccess()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Sửa task") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Đóng") }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.taskDeletedRemotely -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Task này không còn tồn tại")
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::onTitleChange,
                            label = { Text("Tiêu đề") },
                            isError = uiState.titleError != null,
                            supportingText = { uiState.titleError?.let { Text(it) } },
                            singleLine = true,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label = { Text("Mô tả (không bắt buộc)") },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.submitError?.let { error ->
                            Text(text = error.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = viewModel::onSubmit,
                            enabled = !uiState.isSubmitting && uiState.title.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Lưu")
                            }
                        }
                    }
                }
            }
        }
    }
}
