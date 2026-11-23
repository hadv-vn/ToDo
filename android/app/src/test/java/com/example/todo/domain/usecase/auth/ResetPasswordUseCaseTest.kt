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
 * Unit test cho [ResetPasswordUseCase] — theo LLD mục 4.1: token non-empty, password rule,
 * confirm khớp — validate TRƯỚC khi gọi Repository. `confirmNewPassword` CHỈ validate ở client
 * (auth-core `ResetPasswordDto` không nhận field này).
 */
class ResetPasswordUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = ResetPasswordUseCase(authRepository)

    @Test
    fun `token rong thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("", "password1", "password1")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.resetPassword(any(), any()) }
    }

    @Test
    fun `password yeu thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("token-abc", "short1", "short1") // 6 ký tự, chưa đủ 8

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.resetPassword(any(), any()) }
    }

    @Test
    fun `confirmNewPassword khong khop thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("token-abc", "password1", "password2")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.resetPassword(any(), any()) }
    }

    @Test
    fun `input hop le thi goi Repository voi token da trim, khong gui confirmNewPassword`() = runTest {
        coEvery { authRepository.resetPassword("token-abc", "password1") } returns
            ApiResult.Success(Unit)

        // Token có khoảng trắng đầu/cuối -> UseCase phải trim trước khi gọi Repository.
        val result = useCase("  token-abc  ", "password1", "password1")

        assertEquals(ApiResult.Success(Unit), result)
        coVerify(exactly = 1) { authRepository.resetPassword("token-abc", "password1") }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error, khong nuot loi`() = runTest {
        coEvery { authRepository.resetPassword("token-abc", "password1") } returns
            ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Token không hợp lệ hoặc đã hết hạn")

        val result = useCase("token-abc", "password1", "password1")

        assertEquals(
            ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Token không hợp lệ hoặc đã hết hạn"),
            result
        )
    }
}
