package com.example.todo.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.todo.core.session.SessionEvent
import com.example.todo.presentation.auth.ForgotPasswordScreen
import com.example.todo.presentation.auth.LoginScreen
import com.example.todo.presentation.auth.RegisterScreen
import com.example.todo.presentation.auth.ResetPasswordScreen
import com.example.todo.presentation.taskform.CreateTaskScreen
import com.example.todo.presentation.taskform.EditTaskScreen
import com.example.todo.presentation.tasklist.TaskListScreen
import kotlinx.coroutines.launch

/**
 * NavHost gốc nối đủ 7 route theo LLD mục 5. Start destination là [Routes.LOGIN].
 *
 * Phase 2 (Task CRUD) — nối hành vi điều hướng thật CreateTask/EditTask <-> TaskList:
 * - TaskList -> CreateTask/EditTask: navigate thường (thêm vào backstack).
 * - CreateTask/EditTask lưu thành công (hoặc EditTask phát hiện task đã bị xóa ở nơi khác, LLD
 *   mục 4.4d) -> set `Routes.TASK_LIST_REFRESH_NEEDED_KEY = true` trên
 *   `previousBackStackEntry.savedStateHandle` RỒI `popBackStack()`.
 *
 *   QUYẾT ĐỊNH ĐÃ CHỐT (mục 10 đề bài Phase 2): dùng cơ chế savedStateHandle trên
 *   NavBackStackEntry (được LLD mục 6.3 gợi ý) thay vì để TaskListViewModel tự `loadTasks()` lại
 *   mỗi khi màn hình resume (qua Lifecycle ON_RESUME) — vì (1) ON_RESUME có thể bắn nhiều lần/khó
 *   kiểm soát thứ tự khi có animation điều hướng, dễ gây load thừa hoặc miss; (2) savedStateHandle
 *   là API ổn định, được chính NavBackStackEntry của TASK_LIST sở hữu (không cần ViewModel nào
 *   biết về NavController), dễ test/dự đoán hành vi hơn; (3) tránh refresh "giả" khi user chỉ đơn
 *   giản rời App rồi quay lại (không qua Create/Edit) — ON_RESUME sẽ refresh cả trường hợp này
 *   (không sai nhưng lãng phí 1 network call không cần thiết so với cách tiếp cận có chủ đích này).
 *
 * Phase 4 (hardening) — observer 401 toàn cục theo LLD mục 4.4(c)/SRS FR-04 edge case #9:
 * - [SessionObserverViewModel] (scope Activity, tạo ở ĐÂY — ngoài mọi `composable()` destination
 *   — để sống suốt đời app, không bị hủy theo backstack entry) expose lại
 *   `SessionEventBus.events`. `LaunchedEffect(Unit)` gắn ở NavGraph (không phải 1 route cụ thể) ->
 *   collect được event dù 401 xảy ra ở route nào.
 * - `snackbarHostState` đặt ở `Box` bọc NGOÀI `NavHost` (không dùng `Scaffold` để tránh áp thêm
 *   `contentWindowInsets` chồng lên insets mà từng screen con đã tự xử lý qua `Scaffold` riêng của
 *   nó) -> Snackbar hiển thị ở đây SỐNG SÓT qua lần `navController.navigate()` vì nó không thuộc
 *   nội dung của NavHost, không bị thay thế khi đổi destination.
 * - Khi nhận `SessionExpired`: xác định `currentDestination?.route` NGAY TRƯỚC khi navigate (route
 *   pattern, ví dụ "edit_task/{taskId}", so khớp trực tiếp với hằng số `Routes.EDIT_TASK` — không
 *   cần giá trị taskId thật) để chọn đúng message cảnh báo (mất dữ liệu form hay message thường),
 *   rồi `navigate(Routes.LOGIN) { popUpTo(0){inclusive=true}; launchSingleTop=true }` xóa toàn bộ
 *   backstack (đúng pseudocode LLD 4.4c), và `launch{}` show snackbar KHÔNG await (fire-and-forget)
 *   để không làm chậm việc điều hướng về Login.
 */
@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionObserverViewModel: SessionObserverViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        sessionObserverViewModel.sessionEvents.collect { event ->
            when (event) {
                SessionEvent.SessionExpired -> {
                    // Route pattern (vd "edit_task/{taskId}"), không phải route đã điền taskId
                    // thật -> so khớp trực tiếp với hằng số Routes, không cần đọc taskId.
                    val currentRoute = navController.currentDestination?.route
                    val isInTaskForm = currentRoute == Routes.CREATE_TASK || currentRoute == Routes.EDIT_TASK
                    val message = if (isInTaskForm) {
                        "Phiên đăng nhập đã hết hạn. Dữ liệu bạn đang nhập sẽ không được lưu, vui lòng đăng nhập lại."
                    } else {
                        "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
                    }
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                    launch { snackbarHostState.showSnackbar(message) }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavGraphHost(navController = navController)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun NavGraphHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToTaskList = {
                    navController.navigate(Routes.TASK_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateToResetPassword = { email -> navController.navigate(Routes.resetPassword(email)) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.RESET_PASSWORD,
            arguments = listOf(navArgument(Routes.RESET_PASSWORD_ARG_EMAIL) { type = NavType.StringType })
        ) {
            ResetPasswordScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.RESET_PASSWORD) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.TASK_LIST) { backStackEntry ->
            val refreshNeeded by backStackEntry.savedStateHandle
                .getStateFlow(Routes.TASK_LIST_REFRESH_NEEDED_KEY, false)
                .collectAsStateWithLifecycle()
            TaskListScreen(
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToCreateTask = { navController.navigate(Routes.CREATE_TASK) },
                onNavigateToEditTask = { taskId -> navController.navigate(Routes.editTask(taskId)) },
                refreshNeeded = refreshNeeded,
                onRefreshConsumed = {
                    backStackEntry.savedStateHandle[Routes.TASK_LIST_REFRESH_NEEDED_KEY] = false
                }
            )
        }
        composable(Routes.CREATE_TASK) {
            CreateTaskScreen(
                onNavigateBack = { navController.popBackStack() },
                onSubmitSuccess = {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(Routes.TASK_LIST_REFRESH_NEEDED_KEY, true)
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.EDIT_TASK,
            arguments = listOf(navArgument(Routes.EDIT_TASK_ARG_TASK_ID) { type = NavType.StringType })
        ) {
            // taskId KHÔNG cần đọc ở đây -> TaskFormViewModel tự đọc từ SavedStateHandle do Hilt
            // Navigation Compose tự gắn (giống ResetPasswordViewModel đọc "email") -- xem
            // TaskFormViewModel.kt. Không còn SelectedTaskHolder (đã loại bỏ, LLD mục 5/10.3).
            EditTaskScreen(
                onNavigateBack = { navController.popBackStack() },
                onSubmitSuccess = {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(Routes.TASK_LIST_REFRESH_NEEDED_KEY, true)
                    navController.popBackStack()
                }
            )
        }
    }
}
