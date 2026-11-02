package com.example.todo.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginResultDto(
    val user: AuthUserDto,
    val session: SessionInfoDto
)
