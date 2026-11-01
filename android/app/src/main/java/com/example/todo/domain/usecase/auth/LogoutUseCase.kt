package com.example.todo.domain.usecase.auth

import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — không có validate, luôn thành công (chỉ xóa token cục bộ, không gọi API vì
 * không có /auth/logout).
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
