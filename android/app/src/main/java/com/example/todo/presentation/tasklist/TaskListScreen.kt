package com.example.todo.presentation.tasklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todo.domain.model.TaskFilter
import com.example.todo.domain.model.TaskStatus
import com.example.todo.presentation.common.UiError

/**
 * Theo LLD mục 4.2/6.4 — implementation thật Phase 2, bổ sung Phase 3 (FilterChip, pull-to-refresh,
 * empty/error/loading state hoàn chỉnh — LLD mục 4.4(e)/4.4(f), SRS FR-06..FR-10).
 *
 * - [refreshNeeded]/[onRefreshConsumed]: cờ refresh sau khi Create/Edit lưu thành công — xem
 *   Routes.TASK_LIST_REFRESH_NEEDED_KEY/NavGraph.kt để biết lý do chọn cơ chế savedStateHandle
 *   trên NavBackStackEntry thay vì dựa vào Lifecycle ON_RESUME.
 * - Swipe-to-delete dùng SwipeToDismissBox (Material3) + AlertDialog xác nhận; `confirmValueChange`
 *   LUÔN trả `false` (không bao giờ tự commit dismiss) — vị trí swipe luôn tự reset về ban đầu bất
 *   kể người dùng xác nhận hay hủy, đúng nguyên tắc online-only (KHÔNG xóa UI trước khi server xác
 *   nhận — chỉ `onDeleteTaskConfirmed` mới thực sự xóa, sau khi server trả thành công/404).
 * - FilterChip row luôn hiển thị phía trên (ngoài vùng loading/error/empty/list) để người dùng vẫn
 *   đổi được filter ngay cả khi đang ở error-state của filter khác.
 * - Pull-to-refresh dùng `PullToRefreshBox` (androidx.compose.material3.pulltorefresh) — có sẵn từ
 *   Compose BOM 2024.10.00 (material3 ~1.3.x) đã khai báo ở `gradle/libs.versions.toml`, không cần
 *   nâng version. Chỉ bọc vùng list/empty-state — KHÔNG bọc loading/error-state lần tải đầu (những
 *   trạng thái đó dùng nút "Thử lại" riêng, không có khái niệm "danh sách" để kéo refresh).
 * - Loading-state (spinner full-screen) CHỈ khi `isLoading && tasks.isEmpty()` (lần tải đầu) — phân
 *   biệt với `isRefreshing` (đã có indicator riêng của `PullToRefreshBox`, không che toàn màn hình).
 * - Error-state full-screen CHỈ khi lần tải đầu lỗi (`error != null && tasks.isEmpty() && !isLoading`)
 *   kèm nút "Thử lại" (`onRetryClicked`). Lỗi khi đã có `tasks` (kể cả lỗi pull-to-refresh) chỉ hiện
 *   Snackbar ngắn (xem `LaunchedEffect(uiState.error)` bên dưới) — KHÔNG thay UI chính, đúng AC FR-08.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit = {},
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToEditTask: (String) -> Unit = {},
    refreshNeeded: Boolean = false,
    onRefreshConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshNeeded) {
        if (refreshNeeded) {
            viewModel.loadTasks()
            onRefreshConsumed()
        }
    }

    // Toast-like: chỉ dùng Snackbar khi đang có task hiển thị (trạng thái lỗi full-screen bên dưới
    // đã tự hiển thị message inline + nút Thử lại, không cần Snackbar chồng lên).
    LaunchedEffect(uiState.error) {
        val currentError = uiState.error
        if (currentError != null && uiState.tasks.isNotEmpty()) {
            snackbarHostState.showSnackbar(currentError.message)
            viewModel.onErrorConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Task của tôi") },
                actions = {
                    TextButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Text("Đăng xuất")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateTask) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterChipRow(
                currentFilter = uiState.filter,
                onFilterSelected = viewModel::onFilterSelected
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.tasks.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.error != null && uiState.tasks.isEmpty() && !uiState.isLoading -> {
                        ErrorStateView(
                            error = uiState.error,
                            onRetry = viewModel::onRetryClicked
                        )
                    }
                    else -> {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = viewModel::onPullToRefresh,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.tasks.isEmpty() && uiState.emptyStateType != null) {
                                EmptyStateView(
                                    emptyStateType = uiState.emptyStateType!!,
                                    filter = uiState.filter
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(uiState.tasks, key = { it.id }) { task ->
                                        TaskRow(
                                            task = task,
                                            onToggle = { viewModel.onToggleTask(task.id) },
                                            onClick = { onNavigateToEditTask(task.id) },
                                            onDeleteConfirmed = { viewModel.onDeleteTaskConfirmed(task.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** FR-07 (SRS) — FilterChip hàng ngang All/Open/Done phía trên danh sách. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    currentFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == TaskFilter.ALL,
            onClick = { onFilterSelected(TaskFilter.ALL) },
            label = { Text("Tất cả") }
        )
        FilterChip(
            selected = currentFilter == TaskFilter.OPEN,
            onClick = { onFilterSelected(TaskFilter.OPEN) },
            label = { Text("Đang mở") }
        )
        FilterChip(
            selected = currentFilter == TaskFilter.DONE,
            onClick = { onFilterSelected(TaskFilter.DONE) },
            label = { Text("Đã xong") }
        )
    }
}

/**
 * Error-state full-screen khi lần tải ĐẦU TIÊN lỗi (chưa có `tasks` nào để hiển thị) — FR-10 (SRS).
 * Phân biệt lỗi mạng theo [UiError.isNetworkError] để dùng đúng thông điệp SRS gợi ý.
 */
@Composable
private fun ErrorStateView(error: UiError?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (error?.isNetworkError == true) {
                "Không có kết nối mạng"
            } else {
                error?.message ?: "Đã có lỗi xảy ra, vui lòng thử lại"
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Thử lại")
        }
    }
}

/**
 * 2 loại empty-state phân biệt rõ — LLD mục 4.4(f)/FR-09 (SRS). Nội dung text theo đúng gợi ý SRS
 * (bao gồm nguyên văn thông điệp FR-09 cho [EmptyStateType.NO_TASKS_AT_ALL]).
 */
@Composable
private fun EmptyStateView(emptyStateType: EmptyStateType, filter: TaskFilter) {
    val icon = when (emptyStateType) {
        EmptyStateType.NO_TASKS_AT_ALL -> Icons.Default.Info
        EmptyStateType.NO_TASKS_MATCH_FILTER -> Icons.Default.Done
    }
    val message = when (emptyStateType) {
        EmptyStateType.NO_TASKS_AT_ALL -> "Bạn chưa có task nào. Nhấn + để tạo task đầu tiên."
        EmptyStateType.NO_TASKS_MATCH_FILTER -> when (filter) {
            TaskFilter.OPEN -> "Không có task nào đang mở."
            TaskFilter.DONE -> "Không có task nào đã xong."
            // Defensive — không xảy ra thực tế: computeEmptyState (LLD 4.4f) luôn trả
            // NO_TASKS_AT_ALL khi filter == ALL, không bao giờ NO_TASKS_MATCH_FILTER.
            TaskFilter.ALL -> "Không có task nào."
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskUiModel,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // confirmValueChange LUÔN trả false: không bao giờ tự commit việc xóa qua vị trí swipe, chỉ
    // dùng swipe như cử chỉ để MỞ dialog xác nhận -> vị trí swipe tự động animate reset về Settled
    // dù người dùng xác nhận hay hủy (đúng yêu cầu "hủy -> reset vị trí swipe về ban đầu", và cũng
    // tự nhiên đúng với "xác nhận" vì ta không xóa optimistic khỏi UI).
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            false
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa task") },
            text = { Text("Bạn có chắc muốn xóa task này? Hành động không thể hoàn tác.") },
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

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("Xóa", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.isTogglePending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Checkbox(
                    checked = task.status == TaskStatus.DONE,
                    onCheckedChange = { onToggle() }
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = task.title,
                    style = if (task.status == TaskStatus.DONE) {
                        MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    }
                )
                task.descriptionPreview?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
