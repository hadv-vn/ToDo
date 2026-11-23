package com.example.todo.domain.usecase.auth

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.model.AuthUser
import com.example.todo.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho [LoginUseCase] — theo LLD mục 4.1: chỉ check non-empty (login KHÔNG enforce
 * format email, khác [RegisterUseCase]/[ForgotPasswordUseCase] — xem comment trong UseCase).
 */
class LoginUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = LoginUseCase(authRepository)

    private val session = AuthSession(user = AuthUser(id = "user-1", email = "a@b.com"))

    @Test
    fun `email rong thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("", "password1")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `password rong thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("a@b.com", "")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `email va password khong rong thi goi Repository, khong tu validate format`() = runTest {
        // Login không enforce format email (khác Register) -> truyền nguyên văn, không trim.
        coEvery { authRepository.login("khong-phai-email", "password1") } returns
            ApiResult.Success(session)

        val result = useCase("khong-phai-email", "password1")

        assertEquals(ApiResult.Success(session), result)
        coVerify(exactly = 1) { authRepository.login("khong-phai-email", "password1") }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error, khong nuot loi`() = runTest {
        coEvery { authRepository.login("a@b.com", "wrong-pass") } returns
            ApiResult.Error(ApiErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không đúng")

        val result = useCase("a@b.com", "wrong-pass")

        assertEquals(
            ApiResult.Error(ApiErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
            result
        )
    }
}
