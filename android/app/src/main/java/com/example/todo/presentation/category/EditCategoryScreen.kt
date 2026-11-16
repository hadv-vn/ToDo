package com.example.todo.presentation.category

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
 * FR-22 (docs/Task_Category_ToDo.md) — dùng chung [CategoryFormViewModel] với CreateCategoryScreen.
 * `categoryDeletedRemotely`: danh mục đã bị xóa ở nơi khác giữa lúc mở list và mở màn Sửa — tự
 * điều hướng quay lại qua [onSubmitSuccess] (cùng pattern `EditTaskScreen.taskDeletedRemotely`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoryFormViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onSubmitSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSubmitSuccess) {
        if (uiState.isSubmitSuccess) onSubmitSuccess()
    }
    LaunchedEffect(uiState.categoryDeletedRemotely) {
        if (uiState.categoryDeletedRemotely) onSubmitSuccess()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Sửa danh mục") },
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
                uiState.categoryDeletedRemotely -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Danh mục này không còn tồn tại")
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            label = { Text("Tên danh mục") },
                            isError = uiState.nameError != null,
                            supportingText = { uiState.nameError?.let { Text(it) } },
                            singleLine = true,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        CategoryAppearancePicker(
                            selectedColor = uiState.color,
                            selectedIcon = uiState.icon,
                            onColorSelected = viewModel::onColorSelected,
                            onIconSelected = viewModel::onIconSelected
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        uiState.submitError?.let { error ->
                            Text(text = error.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = viewModel::onSubmit,
                            enabled = !uiState.isSubmitting && uiState.name.isNotBlank(),
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
