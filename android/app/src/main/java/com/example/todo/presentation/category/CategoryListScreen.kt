package com.example.todo.presentation.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.domain.model.Category

/** FR-22 (docs/Task_Category_ToDo.md mục 6.1) — màn hình Quản lý danh mục. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoryListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToCreateCategory: () -> Unit = {},
    onNavigateToEditCategory: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error.message)
            viewModel.onErrorConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Quản lý danh mục") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Đóng") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateCategory) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.categories.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.categories.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Bạn chưa có danh mục nào. Nhấn + để tạo danh mục đầu tiên.",
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.categories, key = { it.id }) { category ->
                            CategoryRow(
                                category = category,
                                isDeletePending = category.id in uiState.pendingDeleteCategoryIds,
                                onClick = { onNavigateToEditCategory(category.id) },
                                onDeleteConfirmed = { viewModel.onDeleteCategoryConfirmed(category.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isDeletePending: Boolean,
    onClick: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa danh mục") },
            text = {
                Text(
                    "Bạn có chắc muốn xóa danh mục \"${category.name}\"? " +
                        "Task đang gán danh mục này sẽ chuyển về \"Không danh mục\"."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteConfirmed()
                }) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = Color(category.color.toColorInt()), shape = CircleShape)
        )
        Text(
            text = "${category.icon} ${category.name}",
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        )
        if (isDeletePending) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            TextButton(onClick = { showDeleteDialog = true }) {
                Text("Xóa", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
