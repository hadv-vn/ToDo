package com.example.todo.domain.usecase.auth

import com.example.todo.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit test cho [LogoutUseCase] — theo LLD mục 4.1: không có validate, chỉ gọi thẳng
 * `AuthRepository.logout()` (Repository chịu trách nhiệm gọi API thật rồi luôn xóa session cookie
 * cục bộ, xem [AuthRepositoryImpl] — không phải việc của UseCase này).
 */
class LogoutUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = LogoutUseCase(authRepository)

    @Test
    fun `goi dung mot lan Repository logout`() = runTest {
        coEvery { authRepository.logout() } returns Unit

        useCase()

        coVerify(exactly = 1) { authRepository.logout() }
    }
}
