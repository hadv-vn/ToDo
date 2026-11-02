package com.example.todo.data.mapper

import com.example.todo.data.remote.dto.auth.AuthUserDto
import com.example.todo.domain.model.AuthUser

/**
 * AuthUserDto <-> AuthUser — theo LLD mục 3.5. `role` (auth-core) không map vì Domain Entity
 * [AuthUser] (Phase 0) chưa cần field này cho logic Auth hiện tại.
 */
fun AuthUserDto.toDomain(): AuthUser = AuthUser(id = id, email = email)
