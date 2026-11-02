package com.example.todo.data.remote.api

import com.example.todo.data.remote.dto.task.CreateTaskRequestDto
import com.example.todo.data.remote.dto.task.TaskEnvelopeDto
import com.example.todo.data.remote.dto.task.TaskListEnvelopeDto
import com.example.todo.data.remote.dto.task.UpdateTaskDetailsRequestDto
import com.example.todo.data.remote.dto.task.UpdateTaskStatusRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface cho nhóm endpoint `api/v1/tasks` của auth-core — path/method theo
 * `backend/src/features/todo-task-module/task.controller.ts`. Session xác thực qua cookie
 * (OkHttp CookieJar tự gắn, xem NetworkModule), KHÔNG cần header thủ công như Bearer JWT cũ.
 *
 * Mọi response thành công bọc `{ data: ... }` (xem [TaskEnvelopeDto]/[TaskListEnvelopeDto]) —
 * khác bare JSON của backend Express cũ.
 *
 * LƯU Ý `deleteTask` trả `Response<Void>` (java.lang.Void) THAY VÌ `Response<Unit>` như pseudocode
 * gốc ở LLD mục 3.5 — đây là một điều chỉnh cố ý so với pseudocode (LLD ghi rõ pseudocode "không
 * phải code cuối cùng"):
 * - `204 No Content` (task.controller.ts `remove()`) thực sự KHÔNG có body nào cả.
 * - Converter kotlinx.serialization (dùng cho mọi DTO khác, xem NetworkModule) sẽ gọi
 *   `Json.decodeFromString<Unit>("")` trên body rỗng -> throw SerializationException, bị
 *   `safeApiCall` nuốt thành `ApiResult.Error(UNKNOWN,...)` SAI LỆCH một response 204 THÀNH CÔNG
 *   thành lỗi.
 * - Retrofit có `BuiltInConverters` xử lý riêng cho type `Void` (luôn được kiểm tra TRƯỚC mọi
 *   converter factory tùy biến) — chỉ đóng body và trả `null`, KHÔNG parse gì cả -> an toàn tuyệt
 *   đối với response không có body thật. `TaskRepositoryImpl.deleteTask` bỏ qua giá trị `null` này,
 *   chỉ cần biết request `isSuccessful` hay không.
 */
interface TaskApiService {

    @GET("api/v1/tasks")
    suspend fun getTasks(@Query("status") status: String?): Response<TaskListEnvelopeDto>

    @GET("api/v1/tasks/{id}")
    suspend fun getTaskById(@Path("id") id: String): Response<TaskEnvelopeDto>

    @POST("api/v1/tasks")
    suspend fun createTask(@Body body: CreateTaskRequestDto): Response<TaskEnvelopeDto>

    /** Dùng bởi `editTask` — sửa title/description, KHÔNG đổi status. Xem [UpdateTaskDetailsRequestDto]
     * để biết lý do tách DTO riêng khỏi [updateTaskStatus] (tránh lỗi âm thầm khi xóa description). */
    @PUT("api/v1/tasks/{id}")
    suspend fun updateTaskDetails(
        @Path("id") id: String,
        @Body body: UpdateTaskDetailsRequestDto
    ): Response<TaskEnvelopeDto>

    /** Dùng bởi `toggleTaskStatus` — chỉ đổi status, KHÔNG đụng title/description. */
    @PUT("api/v1/tasks/{id}")
    suspend fun updateTaskStatus(
        @Path("id") id: String,
        @Body body: UpdateTaskStatusRequestDto
    ): Response<TaskEnvelopeDto>

    @DELETE("api/v1/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Void>
}
