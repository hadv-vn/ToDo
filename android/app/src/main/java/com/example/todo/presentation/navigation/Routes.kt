package com.example.todo.presentation.navigation

/**
 * Định nghĩa route cho Navigation Compose — theo LLD mục 5.
 *
 * `reset_password/{email}` và `edit_task/{taskId}` có tham số bắt buộc trong path;
 * [resetPassword]/[editTask] là helper tạo route cụ thể để navigate tới (đã gắn giá trị thật).
 */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password/{email}"
    const val TASK_LIST = "task_list"
    const val CREATE_TASK = "create_task"
    const val EDIT_TASK = "edit_task/{taskId}"

    const val RESET_PASSWORD_ARG_EMAIL = "email"
    const val EDIT_TASK_ARG_TASK_ID = "taskId"

    // Key của SavedStateHandle trên NavBackStackEntry của TASK_LIST — set = true bởi
    // CreateTask/EditTask (trên `previousBackStackEntry`) ngay trước khi popBackStack() sau khi
    // lưu thành công (hoặc EditTask phát hiện task đã bị xóa ở nơi khác) để báo TaskListScreen tự
    // loadTasks() lại (xem LLD mục 5/6.3, "Luồng tải dữ liệu EditTaskScreen" + NavGraph.kt).
    const val TASK_LIST_REFRESH_NEEDED_KEY = "refreshNeeded"

    fun resetPassword(email: String): String = "reset_password/$email"
    fun editTask(taskId: String): String = "edit_task/$taskId"
}
