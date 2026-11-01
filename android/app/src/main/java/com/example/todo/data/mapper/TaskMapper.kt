package com.example.todo.data.mapper

import com.example.todo.data.remote.dto.task.CreateTaskRequestDto
import com.example.todo.data.remote.dto.task.TaskDto
import com.example.todo.domain.model.Task
import com.example.todo.domain.model.TaskStatus
import java.time.Instant

/**
 * TaskDto <-> Task — theo LLD mục 3.5/4.3. Parse `status` string -> enum, parse chuỗi ISO-8601 ->
 * `java.time.Instant` (quyết định Phase 0 — xem domain/model/Task.kt, nhất quán xuyên suốt, không
 * dùng kotlin.time.Instant).
 */
fun TaskDto.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    status = status.toDomainTaskStatus(),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt)
)

/**
 * Map ngược khi cần gửi Task (Domain) lên server dưới dạng tạo mới. Dùng cho trường hợp hiếm cần
 * resend một Task Domain đã có sẵn (ví dụ retry); luồng Create chính (TaskFormViewModel) gọi
 * CreateTaskUseCase(title, description) trực tiếp từ input người dùng, không qua hàm này.
 */
fun Task.toCreateRequestDto(): CreateTaskRequestDto = CreateTaskRequestDto(title = title, description = description)

private fun String.toDomainTaskStatus(): TaskStatus = when (this) {
    "done" -> TaskStatus.DONE
    else -> TaskStatus.OPEN // "open" hoặc giá trị lạ -> coi như OPEN (defensive; server đã validate enum)
}

fun TaskStatus.toWireValue(): String = when (this) {
    TaskStatus.OPEN -> "open"
    TaskStatus.DONE -> "done"
}
