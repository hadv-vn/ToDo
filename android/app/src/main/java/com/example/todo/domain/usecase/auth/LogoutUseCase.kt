package com.example.todo.domain.usecase.auth

import com.example.todo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Theo LLD mục 4.1 — không có validate. Gọi `POST /auth/logout` thật (revoke session server-side
 * của auth-core) rồi luôn xóa session cookie cục bộ, xem [AuthRepositoryImpl].
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
