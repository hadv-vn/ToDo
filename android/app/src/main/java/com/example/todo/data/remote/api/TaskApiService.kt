package com.example.todo.data.remote.api

import com.example.todo.data.remote.dto.task.CreateTaskRequestDto
import com.example.todo.data.remote.dto.task.TaskDto
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
 * Retrofit interface cho nhóm endpoint /tasks — path/method theo API_Contract_ToDo_Android.md mục 3. Mọi
 * request được [com.example.todo.data.remote.interceptor.AuthHeaderInterceptor] tự gắn
 * `Authorization: Bearer <token>` (path bắt đầu "/tasks" — LLD mục 3.7).
 *
 * LƯU Ý `deleteTask` trả `Response<Void>` (java.lang.Void) THAY VÌ `Response<Unit>` như pseudocode
 * gốc ở LLD mục 3.5 — đây là một điều chỉnh cố ý so với pseudocode (LLD ghi rõ pseudocode "không
 * phải code cuối cùng"):
 * - `204 No Content` (API_Contract mục 3.5) thực sự KHÔNG có body nào cả.
 * - Converter kotlinx.serialization (dùng cho mọi DTO khác, xem NetworkModule) sẽ gọi
 *   `Json.decodeFromString<Unit>("")` trên body rỗng -> throw SerializationException, bị
 *   `safeApiCall` nuốt thành `ApiResult.Error(UNKNOWN,...)` SAI LỆCH một response 204 THÀNH CÔNG
 *   thành lỗi. Khác với `forgotPassword`/`resetPassword` ở AuthApiService (cũng khai `Response<Unit>`)
 *   vì 2 endpoint đó LUÔN có body JSON thật `{"message": "..."}`, decode Unit từ 1 JSON object hợp
 *   lệ không gặp vấn đề này.
 * - Retrofit có `BuiltInConverters` xử lý riêng cho type `Void` (luôn được kiểm tra TRƯỚC mọi
 *   converter factory tùy biến) — chỉ đóng body và trả `null`, KHÔNG parse gì cả -> an toàn tuyệt
 *   đối với response không có body thật. `TaskRepositoryImpl.deleteTask` bỏ qua giá trị `null` này,
 *   chỉ cần biết request `isSuccessful` hay không.
 */
interface TaskApiService {

    @GET("tasks")
    suspend fun getTasks(@Query("status") status: String?): Response<List<TaskDto>>

    @GET("tasks/{id}")
    suspend fun getTaskById(@Path("id") id: String): Response<TaskDto>

    @POST("tasks")
    suspend fun createTask(@Body body: CreateTaskRequestDto): Response<TaskDto>

    /** Dùng bởi `editTask` — sửa title/description, KHÔNG đổi status. Xem [UpdateTaskDetailsRequestDto]
     * để biết lý do tách DTO riêng khỏi [updateTaskStatus] (tránh lỗi âm thầm khi xóa description). */
    @PUT("tasks/{id}")
    suspend fun updateTaskDetails(
        @Path("id") id: String,
        @Body body: UpdateTaskDetailsRequestDto
    ): Response<TaskDto>

    /** Dùng bởi `toggleTaskStatus` — chỉ đổi status, KHÔNG đụng title/description. */
    @PUT("tasks/{id}")
    suspend fun updateTaskStatus(
        @Path("id") id: String,
        @Body body: UpdateTaskStatusRequestDto
    ): Response<TaskDto>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Void>
}
