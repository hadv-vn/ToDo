package com.example.todo.domain.usecase.auth

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho [ForgotPasswordUseCase] — theo LLD mục 4.1: validate email format TRƯỚC khi gọi
 * Repository, cùng rule với [RegisterUseCase]/[ResendVerificationUseCase].
 */
class ForgotPasswordUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = ForgotPasswordUseCase(authRepository)

    @Test
    fun `email sai dinh dang thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("khong-phai-email")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.forgotPassword(any()) }
    }

    @Test
    fun `email hop le thi goi Repository voi email da trim`() = runTest {
        coEvery { authRepository.forgotPassword("a@b.com") } returns ApiResult.Success(Unit)

        val result = useCase("  a@b.com  ")

        assertEquals(ApiResult.Success(Unit), result)
        coVerify(exactly = 1) { authRepository.forgotPassword("a@b.com") }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error, khong nuot loi`() = runTest {
        coEvery { authRepository.forgotPassword("a@b.com") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Email không tồn tại")

        val result = useCase("a@b.com")

        assertEquals(ApiResult.Error(ApiErrorCode.NOT_FOUND, "Email không tồn tại"), result)
    }
}
