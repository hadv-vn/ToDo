package com.example.todo.presentation.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskFilter
import com.example.todo.domain.model.TaskStatus
import com.example.todo.domain.usecase.auth.LogoutUseCase
import com.example.todo.domain.usecase.task.DeleteTaskUseCase
import com.example.todo.domain.usecase.task.GetTasksUseCase
import com.example.todo.domain.usecase.task.ToggleTaskStatusUseCase
import com.example.todo.presentation.common.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Theo LLD mục 4.2 (struct)/4.4(a) (double-tap protection + rollback, NGUYÊN VĂN pseudocode)/4.4(d)
 * (xử lý 404 khi toggle/delete task đã bị xóa nơi khác)/4.4(e) (pull-to-refresh, NGUYÊN VĂN
 * pseudocode)/4.4(f) (phân biệt 2 loại empty-state, NGUYÊN VĂN pseudocode).
 *
 * Phase 3: thêm [onFilterSelected] (FilterChip UI), [onPullToRefresh] (pull-to-refresh giữ nguyên
 * filter, KHÔNG xóa `tasks` cũ khi lỗi), [onRetryClicked] (nút "Thử lại" ở error-state khi lần load
 * ĐẦU TIÊN lỗi — phân biệt với pull-to-refresh vì khi đó `tasks` đang rỗng, không có gì để giữ).
 * [computeHasAnyTaskOverall]/[computeEmptyState] đã đúng NGUYÊN VĂN pseudocode LLD 4.4(f) từ Phase 2,
 * không cần sửa. Giữ [logout] từ Phase 1.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks(showLoading: Boolean = true) {
        if (_uiState.value.isLoading) return // tránh chồng nhiều request load cùng lúc
        val filter = _uiState.value.filter
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
        viewModelScope.launch {
            val result = getTasksUseCase(filter)
            when (result) {
                is ApiResult.Success -> {
                    val uiTasks = result.data.map { it.toUiModel(isTogglePending = false) }
                    _uiState.update { state ->
                        val hasAnyTaskOverall = computeHasAnyTaskOverall(state, filter, uiTasks)
                        state.copy(
                            isLoading = false,
                            tasks = uiTasks,
                            hasAnyTaskOverall = hasAnyTaskOverall,
                            emptyStateType = computeEmptyState(uiTasks, filter, hasAnyTaskOverall),
                            error = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError(result.message ?: "Đã có lỗi xảy ra, vui lòng thử lại")
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError("Không có kết nối mạng, vui lòng thử lại", isNetworkError = true)
                        )
                    }
                }
            }
        }
    }

    /**
     * FR-06/FR-07 (SRS) — đổi filter đang áp dụng rồi tải lại theo filter mới.
     *
     * Bỏ qua nếu đang có một lần load dở dang ([TaskListUiState.isLoading]): nếu không chặn, request
     * của lần load CŨ (đã capture `filter` CŨ làm closure local trước khi user tap chip) có thể trả
     * về SAU request mới và đè nhầm state theo filter cũ — tránh hẳn race này bằng cách không cho đổi
     * filter khi đang loading, thay vì thêm cơ chế huỷ/versioning phức tạp không cần thiết cho scope
     * hiện tại.
     */
    fun onFilterSelected(filter: TaskFilter) {
        val current = _uiState.value
        if (current.filter == filter) return
        if (current.isLoading) return
        _uiState.update { it.copy(filter = filter, error = null) }
        // Nếu đang có task hiển thị (filter cũ), đổi filter không che toàn màn hình bằng spinner —
        // giữ danh sách cũ hiển thị trong lúc chờ, giống cảm giác pull-to-refresh. Nếu đang rỗng/lỗi
        // (chưa có gì để giữ), vẫn hiện spinner full-screen như lần tải đầu.
        loadTasks(showLoading = current.tasks.isEmpty())
    }

    /**
     * NGUYÊN VĂN pseudocode LLD mục 4.4(e): giữ nguyên filter đang áp dụng, lỗi thì CHỈ tắt
     * `isRefreshing` + set `error`, KHÔNG xóa `tasks` cũ (đúng AC FR-08 — danh sách cũ giữ nguyên).
     */
    fun onPullToRefresh() {
        if (_uiState.value.isRefreshing) return
        val currentFilter = _uiState.value.filter
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val result = getTasksUseCase(currentFilter)
            when (result) {
                is ApiResult.Success -> {
                    val uiTasks = result.data.map { it.toUiModel(isTogglePending = false) }
                    _uiState.update { state ->
                        val hasAnyTaskOverall = computeHasAnyTaskOverall(state, currentFilter, uiTasks)
                        state.copy(
                            isRefreshing = false,
                            error = null,
                            tasks = uiTasks,
                            hasAnyTaskOverall = hasAnyTaskOverall,
                            emptyStateType = computeEmptyState(uiTasks, currentFilter, hasAnyTaskOverall)
                        )
                    }
                }
                is ApiResult.Error -> {
                    // KHÔNG xóa `tasks` cũ — chỉ tắt indicator + báo lỗi (Snackbar, vì tasks không rỗng).
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = UiError(result.message ?: "Có lỗi xảy ra, vui lòng thử lại")
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = UiError("Không có kết nối mạng, vui lòng thử lại", isNetworkError = true)
                        )
                    }
                }
            }
        }
    }

    /**
     * Nút "Thử lại" ở error-state khi lần load ĐẦU TIÊN lỗi (lúc đó `tasks` đang rỗng — khác
     * [onPullToRefresh] vốn dùng khi đã có `tasks` hiển thị). Tái dùng [loadTasks] với
     * `showLoading = true` (giá trị mặc định) vì không có gì để giữ hiển thị trong lúc chờ.
     */
    fun onRetryClicked() {
        loadTasks()
    }

    /**
     * NGUYÊN VĂN pseudocode LLD mục 4.4(a). Check `isTogglePending` chạy đồng bộ trên Main thread
     * TRƯỚC điểm suspend đầu tiên -> 2 tap liên tiếp luôn xử lý tuần tự, không race.
     */
    fun onToggleTask(taskId: String) {
        val targetTask = _uiState.value.tasks.find { it.id == taskId } ?: return
        if (targetTask.isTogglePending) return // chặn double-tap

        val previousStatus = targetTask.status
        val optimisticStatus = opposite(previousStatus)

        _uiState.update { state ->
            state.copy(tasks = state.tasks.map { t ->
                if (t.id == taskId) t.copy(status = optimisticStatus, isTogglePending = true) else t
            })
        }

        viewModelScope.launch {
            val result = toggleTaskStatusUseCase(taskId, previousStatus)
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(tasks = state.tasks.map { t ->
                            if (t.id == taskId) t.copy(isTogglePending = false) else t
                        })
                    }
                }
                is ApiResult.Error -> {
                    when (result.code) {
                        ApiErrorCode.NOT_FOUND -> {
                            // Loại hẳn khỏi state, KHÔNG rollback status — MỘT update duy nhất
                            // (loại task + set error) để tránh phát 2 state riêng biệt (flicker 1
                            // frame "đã loại task nhưng chưa có thông báo" không cần thiết).
                            _uiState.update { state ->
                                state.copy(
                                    tasks = state.tasks.filterNot { it.id == taskId },
                                    error = UiError("Task không còn tồn tại")
                                )
                            }
                        }
                        ApiErrorCode.FORBIDDEN -> {
                            _uiState.update { state ->
                                state.copy(
                                    tasks = rolledBackTasks(state.tasks, taskId, previousStatus),
                                    error = UiError("Bạn không có quyền thực hiện thao tác này")
                                )
                            }
                        }
                        else -> {
                            _uiState.update { state ->
                                state.copy(
                                    tasks = rolledBackTasks(state.tasks, taskId, previousStatus),
                                    error = UiError(result.message ?: "Có lỗi xảy ra, vui lòng thử lại")
                                )
                            }
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update { state ->
                        state.copy(
                            tasks = rolledBackTasks(state.tasks, taskId, previousStatus),
                            error = UiError("Không có kết nối mạng, vui lòng thử lại", isNetworkError = true)
                        )
                    }
                }
            }
        }
    }

    /**
     * Online-only (LLD mục 6.4/SRS mục 10.1): KHÔNG xóa task khỏi UI trước khi server xác nhận.
     * 404 (đã bị xóa ở nơi khác) coi như đạt mục đích -> vẫn loại khỏi UI, hiển thị thông báo riêng
     * (KHÔNG phải lỗi chung) — LLD mục 4.4(d).
     *
     * Phase 4 hardening: check `pendingDeleteTaskIds` đồng bộ TRƯỚC điểm suspend đầu tiên (cùng
     * pattern [onToggleTask]) để chặn double-confirm (ví dụ dialog xác nhận bị bấm "Xóa" 2 lần liên
     * tiếp do lag UI trước khi dialog kịp đóng).
     */
    fun onDeleteTaskConfirmed(taskId: String) {
        if (taskId in _uiState.value.pendingDeleteTaskIds) return // chặn double-confirm

        _uiState.update { it.copy(pendingDeleteTaskIds = it.pendingDeleteTaskIds + taskId) }

        viewModelScope.launch {
            val result = deleteTaskUseCase(taskId)
            when (result) {
                is ApiResult.Success -> {
                    removeTaskFromState(taskId)
                    clearPendingDelete(taskId)
                }
                is ApiResult.Error -> {
                    when (result.code) {
                        ApiErrorCode.NOT_FOUND -> {
                            // MỘT update duy nhất (loại task + set error), lý do xem onToggleTask.
                            _uiState.update { state ->
                                state.copy(
                                    tasks = state.tasks.filterNot { it.id == taskId },
                                    pendingDeleteTaskIds = state.pendingDeleteTaskIds - taskId,
                                    error = UiError("Task này đã được xóa trước đó")
                                )
                            }
                        }
                        ApiErrorCode.FORBIDDEN -> {
                            _uiState.update {
                                it.copy(
                                    pendingDeleteTaskIds = it.pendingDeleteTaskIds - taskId,
                                    error = UiError("Bạn không có quyền thực hiện thao tác này")
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    pendingDeleteTaskIds = it.pendingDeleteTaskIds - taskId,
                                    error = UiError(result.message ?: "Có lỗi xảy ra, vui lòng thử lại")
                                )
                            }
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            pendingDeleteTaskIds = it.pendingDeleteTaskIds - taskId,
                            error = UiError("Không có kết nối mạng, vui lòng thử lại", isNetworkError = true)
                        )
                    }
                }
            }
        }
    }

    /** UI (Snackbar) gọi sau khi đã hiển thị [TaskListUiState.error] để tránh hiện lại. */
    fun onErrorConsumed() {
        _uiState.update { it.copy(error = null) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }

    // Hàm thuần (không tự update state) — cho phép gộp rollback + set error vào CÙNG MỘT
    // _uiState.update{} ở nơi gọi (tránh phát 2 emission riêng biệt cho 1 lần lỗi).
    private fun rolledBackTasks(
        tasks: List<TaskUiModel>,
        taskId: String,
        previousStatus: TaskStatus
    ): List<TaskUiModel> = tasks.map { t ->
        if (t.id == taskId) t.copy(status = previousStatus, isTogglePending = false) else t
    }

    private fun removeTaskFromState(taskId: String) {
        _uiState.update { state -> state.copy(tasks = state.tasks.filterNot { it.id == taskId }) }
    }

    private fun clearPendingDelete(taskId: String) {
        _uiState.update { it.copy(pendingDeleteTaskIds = it.pendingDeleteTaskIds - taskId) }
    }

    private fun opposite(status: TaskStatus): TaskStatus =
        if (status == TaskStatus.OPEN) TaskStatus.DONE else TaskStatus.OPEN

    // LLD mục 4.4(f).
    private fun computeHasAnyTaskOverall(
        previousState: TaskListUiState,
        filter: TaskFilter,
        newTasks: List<TaskUiModel>
    ): Boolean {
        return if (filter == TaskFilter.ALL) {
            newTasks.isNotEmpty() // ALL là nguồn chân lý tuyệt đối
        } else {
            previousState.hasAnyTaskOverall || newTasks.isNotEmpty() // không hạ cờ nếu đã từng biết có task
        }
    }

    private fun computeEmptyState(
        tasks: List<TaskUiModel>,
        filter: TaskFilter,
        hasAnyTaskOverall: Boolean
    ): EmptyStateType? {
        if (tasks.isNotEmpty()) return null
        if (filter == TaskFilter.ALL) return EmptyStateType.NO_TASKS_AT_ALL
        return if (hasAnyTaskOverall) EmptyStateType.NO_TASKS_MATCH_FILTER else EmptyStateType.NO_TASKS_AT_ALL
    }
}

private const val DESCRIPTION_PREVIEW_MAX_LENGTH = 60

/** Mapper tầng Presentation (LLD mục 4.2) — truncate description về 1 dòng, tối đa 60 ký tự. */
private fun Task.toUiModel(isTogglePending: Boolean): TaskUiModel {
    val preview = description
        ?.lineSequence()?.firstOrNull()
        ?.let { firstLine ->
            if (firstLine.length > DESCRIPTION_PREVIEW_MAX_LENGTH) {
                firstLine.take(DESCRIPTION_PREVIEW_MAX_LENGTH) + "…"
            } else {
                firstLine
            }
        }
        ?.takeIf { it.isNotBlank() }
    return TaskUiModel(
        id = id,
        title = title,
        descriptionPreview = preview,
        status = status,
        isTogglePending = isTogglePending
    )
}
