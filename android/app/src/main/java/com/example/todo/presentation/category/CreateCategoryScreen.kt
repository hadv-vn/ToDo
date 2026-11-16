package com.example.todo.presentation.category

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

/** FR-22 (docs/Task_Category_ToDo.md) — dùng chung [CategoryFormViewModel] với EditCategoryScreen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCategoryScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoryFormViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onSubmitSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSubmitSuccess) {
        if (uiState.isSubmitSuccess) onSubmitSuccess()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Tạo danh mục mới") },
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
