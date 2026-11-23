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
 * Unit test cho [ResendVerificationUseCase] — dùng khi login báo `EMAIL_NOT_VERIFIED` (xem
 * [LoginUseCase]). Validate email format TRƯỚC khi gọi Repository, cùng rule với
 * [RegisterUseCase]/[ForgotPasswordUseCase].
 */
class ResendVerificationUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = ResendVerificationUseCase(authRepository)

    @Test
    fun `email sai dinh dang thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("khong-phai-email")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.resendVerification(any()) }
    }

    @Test
    fun `email hop le thi goi Repository voi email da trim`() = runTest {
        coEvery { authRepository.resendVerification("a@b.com") } returns ApiResult.Success(Unit)

        val result = useCase("  a@b.com  ")

        assertEquals(ApiResult.Success(Unit), result)
        coVerify(exactly = 1) { authRepository.resendVerification("a@b.com") }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error, khong nuot loi`() = runTest {
        coEvery { authRepository.resendVerification("a@b.com") } returns
            ApiResult.Error(ApiErrorCode.CONFLICT, "Email đã được xác thực")

        val result = useCase("a@b.com")

        assertEquals(ApiResult.Error(ApiErrorCode.CONFLICT, "Email đã được xác thực"), result)
    }
}
