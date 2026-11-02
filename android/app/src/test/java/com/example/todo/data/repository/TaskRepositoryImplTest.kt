package com.example.todo.data.repository

import com.example.todo.data.remote.api.TaskApiService
import com.example.todo.data.remote.dto.task.TaskDto
import com.example.todo.data.remote.dto.task.TaskEnvelopeDto
import com.example.todo.data.remote.dto.task.TaskListEnvelopeDto
import com.example.todo.data.remote.dto.task.UpdateTaskDetailsRequestDto
import com.example.todo.data.remote.dto.task.UpdateTaskStatusRequestDto
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Unit test cho [TaskRepositoryImpl] — tập trung vào điểm thiết kế quan trọng ở LLD mục 3.4/9.3:
 * [TaskRepositoryImpl.editTask] và [TaskRepositoryImpl.toggleTaskStatus] CÙNG gọi PUT /tasks/:id
 * nhưng qua 2 method Retrofit + 2 DTO RIÊNG ([TaskApiService.updateTaskDetails] /
 * [TaskApiService.updateTaskStatus]) — editTask gửi title+description (KHÔNG đổi status),
 * toggleTaskStatus CHỈ gửi status (KHÔNG đổi title/description). Tách DTO (thay vì dùng chung 1 DTO
 * 3-field-optional như pseudocode LLD gốc) để tránh bug: kotlinx.serialization mặc định lược bỏ
 * field bằng giá trị default (`null`) khỏi JSON, khiến "xóa mô tả" (gửi `description: null`) bị
 * lược bỏ nhầm thành "không gửi field" (giữ nguyên mô tả cũ) nếu dùng DTO dùng chung.
 * Test này tồn tại để tránh regression nếu sau này có người vô tình gộp nhầm 2 method/DTO.
 */
class TaskRepositoryImplTest {

    private val taskApiService: TaskApiService = mockk()
    private val repository = TaskRepositoryImpl(taskApiService)

    private val taskDto = TaskDto(
        id = "task-1",
        title = "Mua sữa tươi",
        description = "2 lít",
        status = "open",
        createdAt = "2026-10-14T08:30:00.000Z",
        updatedAt = "2026-10-14T08:30:00.000Z"
    )

    @Test
    fun `editTask thi goi updateTaskDetails voi title va description`() = runTest {
        coEvery { taskApiService.updateTaskDetails("task-1", any()) } returns
            Response.success(TaskEnvelopeDto(data = taskDto))

        val result = repository.editTask("task-1", "Mua sữa tươi", "2 lít")

        assertTrue(result is ApiResult.Success)
        coVerify(exactly = 1) {
            taskApiService.updateTaskDetails(
                "task-1",
                UpdateTaskDetailsRequestDto(title = "Mua sữa tươi", description = "2 lít")
            )
        }
    }

    @Test
    fun `editTask voi description null thi van gui truong description = null (khong bi luoc bo)`() = runTest {
        coEvery { taskApiService.updateTaskDetails("task-1", any()) } returns
            Response.success(TaskEnvelopeDto(data = taskDto.copy(description = null)))

        val result = repository.editTask("task-1", "Mua sữa tươi", null)

        assertTrue(result is ApiResult.Success)
        coVerify(exactly = 1) {
            taskApiService.updateTaskDetails(
                "task-1",
                UpdateTaskDetailsRequestDto(title = "Mua sữa tươi", description = null)
            )
        }
    }

    @Test
    fun `toggleTaskStatus thi goi updateTaskStatus CHI voi status, khong co truong title hoac description`() = runTest {
        coEvery { taskApiService.updateTaskStatus("task-1", any()) } returns
            Response.success(TaskEnvelopeDto(data = taskDto.copy(status = "done")))

        val result = repository.toggleTaskStatus("task-1", TaskStatus.DONE)

        assertTrue(result is ApiResult.Success)
        assertEquals(TaskStatus.DONE, (result as ApiResult.Success).data.status)
        coVerify(exactly = 1) {
            taskApiService.updateTaskStatus("task-1", UpdateTaskStatusRequestDto(status = "done"))
        }
    }

    @Test
    fun `getTasks thanh cong thi map dung List TaskDto sang List Task`() = runTest {
        coEvery { taskApiService.getTasks(null) } returns Response.success(TaskListEnvelopeDto(data = listOf(taskDto)))

        val result = repository.getTasks(filter = null)

        assertTrue(result is ApiResult.Success)
        val tasks = (result as ApiResult.Success).data
        assertEquals(1, tasks.size)
        assertEquals("task-1", tasks.first().id)
        assertEquals(TaskStatus.OPEN, tasks.first().status)
    }

    @Test
    fun `deleteTask nhan 404 thi tra Error NOT_FOUND`() = runTest {
        val errorBody = "{\"error\":{\"code\":\"TASK_NOT_FOUND\",\"message\":\"Task không tồn tại\"}}"
            .toResponseBody("application/json".toMediaType())
        coEvery { taskApiService.deleteTask("task-404") } returns Response.error(404, errorBody)

        val result = repository.deleteTask("task-404")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ApiErrorCode.NOT_FOUND, error.code)
        assertEquals("Task không tồn tại", error.message)
    }

    @Test
    fun `deleteTask thanh cong 204 thi tra Success Unit`() = runTest {
        coEvery { taskApiService.deleteTask("task-1") } returns Response.success<Void>(204, null)

        val result = repository.deleteTask("task-1")

        assertTrue(result is ApiResult.Success)
        assertEquals(Unit, (result as ApiResult.Success).data)
    }
}
