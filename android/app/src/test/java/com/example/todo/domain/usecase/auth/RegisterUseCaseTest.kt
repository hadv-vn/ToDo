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
 * Unit test cho [RegisterUseCase] (test riêng, không qua ViewModel) — theo LLD mục 4.1: validate
 * email/password/confirm TRƯỚC khi gọi Repository; invalid -> KHÔNG bao giờ gọi network.
 *
 * `confirmPassword` CHỈ validate ở client — auth-core `RegisterDto` không nhận field này, nên
 * `AuthRepository.register` chỉ nhận `(email, password)` và trả `ApiResult<Unit>`.
 */
class RegisterUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = RegisterUseCase(authRepository)

    @Test
    fun `email sai dinh dang thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("khong-phai-email", "password1", "password1")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.register(any(), any()) }
    }

    @Test
    fun `password yeu thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("a@b.com", "short1", "short1") // 6 ký tự, chưa đủ 8

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.register(any(), any()) }
    }

    @Test
    fun `confirmPassword khong khop thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("a@b.com", "password1", "password2")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.register(any(), any()) }
    }

    @Test
    fun `input hop le thi goi Repository voi email da trim, khong gui confirmPassword, tra ve ket qua Repository`() =
        runTest {
            coEvery { authRepository.register("a@b.com", "password1") } returns ApiResult.Success(Unit)

            // Email có khoảng trắng đầu/cuối -> UseCase phải trim trước khi gọi Repository.
            val result = useCase("  a@b.com  ", "password1", "password1")

            assertEquals(ApiResult.Success(Unit), result)
            coVerify(exactly = 1) { authRepository.register("a@b.com", "password1") }
        }
}
