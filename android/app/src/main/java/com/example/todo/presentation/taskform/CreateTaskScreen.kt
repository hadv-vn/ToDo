package com.example.todo.presentation.taskform

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Theo LLD mục 4.2/6.3 — implementation thật Phase 2 (thay thế placeholder Phase 0). Dùng chung
 * [TaskFormViewModel] với EditTaskScreen (taskId == null ở route này -> Create mode).
 *
 * Submit thành công -> gọi [onSubmitSuccess] (NavGraph.kt: set
 * `Routes.TASK_LIST_REFRESH_NEEDED_KEY` trên previousBackStackEntry rồi popBackStack()).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Tạo task mới") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Đóng") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
