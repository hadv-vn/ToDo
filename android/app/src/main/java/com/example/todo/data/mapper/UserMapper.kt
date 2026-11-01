package com.example.todo.data.mapper

import com.example.todo.data.remote.dto.auth.UserDto
import com.example.todo.domain.model.AuthUser

/**
 * UserDto <-> AuthUser — theo LLD mục 3.5. `createdAt` (API_Contract mục 2.1/2.2) không map vì
 * Domain Entity [AuthUser] (Phase 0) chưa cần field này cho logic Auth hiện tại.
 */
fun UserDto.toDomain(): AuthUser = AuthUser(id = id, email = email)
