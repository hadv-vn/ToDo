package com.example.todo.data.repository

import com.example.todo.data.local.SessionCookieStore
import com.example.todo.data.remote.api.AuthApiService
import com.example.todo.data.remote.dto.auth.AuthUserDto
import com.example.todo.data.remote.dto.auth.LoginResponseDto
import com.example.todo.data.remote.dto.auth.LoginResultDto
import com.example.todo.data.remote.dto.auth.RegisterResponseDto
import com.example.todo.data.remote.dto.auth.RegisteredUserDto
import com.example.todo.data.remote.dto.auth.SessionInfoDto
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.model.AuthUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Unit test cho [AuthRepositoryImpl] — MockK cho [AuthApiService]/[SessionCookieStore], không gọi
 * network thật.
 *
 * Session auth-core xác lập qua `Set-Cookie` (OkHttp CookieJar tự lưu, NGOÀI tầm kiểm soát của
 * Repository) -> register/login KHÔNG còn lưu token tường minh như backend Express cũ; test chỉ
 * xác nhận map đúng DTO envelope `{data:...}` -> Domain, và logout/hasActiveSession tương tác
 * đúng với [SessionCookieStore].
 */
class AuthRepositoryImplTest {

    private val authApiService: AuthApiService = mockk()
    private val sessionCookieStore: SessionCookieStore = mockk()
    private val repository = AuthRepositoryImpl(authApiService, sessionCookieStore)

    @Test
    fun `register thanh cong thi tra Success Unit, khong dong cham session cookie`() = runTest {
        val responseDto = RegisterResponseDto(
            data = RegisteredUserDto(
                id = "u1",
                email = "a@b.com",
                role = "USER",
                status = "PENDING_VERIFICATION",
                createdAt = "2026-10-14T00:00:00.000Z"
            ),
            message = "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản."
        )
        coEvery { authApiService.register(any()) } returns Response.success(responseDto)

        val result = repository.register("a@b.com", "password1")

        assertEquals(ApiResult.Success(Unit), result)
    }

    @Test
    fun `login thanh cong thi tra AuthSession dung du lieu`() = runTest {
        val responseDto = LoginResponseDto(
            data = LoginResultDto(
                user = AuthUserDto(id = "u2", email = "c@d.com", role = "USER"),
                session = SessionInfoDto(id = "sess-1", expiresAt = "2026-10-21T00:00:00.000Z")
            )
        )
        coEvery { authApiService.login(any()) } returns Response.success(responseDto)

        val result = repository.login("c@d.com", "password1")

        val expectedSession = AuthSession(user = AuthUser(id = "u2", email = "c@d.com"))
        assertEquals(ApiResult.Success(expectedSession), result)
    }

    @Test
    fun `register mat mang thi tra NetworkError`() = runTest {
        coEvery { authApiService.register(any()) } throws IOException("timeout")

        val result = repository.register("a@b.com", "password1")

        assertEquals(ApiResult.NetworkError, result)
    }

    @Test
    fun `login sai mat khau 401 thi tra Error UNAUTHORIZED dung errorCode`() = runTest {
        val errorBody = "{\"error\":{\"code\":\"INVALID_CREDENTIALS\",\"message\":\"Email hoặc mật khẩu không đúng\"}}"
            .toResponseBody("application/json".toMediaType())
        coEvery { authApiService.login(any()) } returns Response.error(401, errorBody)

        val result = repository.login("a@b.com", "wrongpass")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ApiErrorCode.UNAUTHORIZED, error.code)
        assertEquals("Email hoặc mật khẩu không đúng", error.message)
        assertEquals("INVALID_CREDENTIALS", error.errorCode)
    }

    @Test
    fun `logout thi goi API logout roi xoa session cookie cuc bo`() = runTest {
        coEvery { authApiService.logout() } returns Response.success(Unit)
        coEvery { sessionCookieStore.clear() } returns Unit

        repository.logout()

        coVerify(exactly = 1) { authApiService.logout() }
        coVerify(exactly = 1) { sessionCookieStore.clear() }
    }

    @Test
    fun `logout mat mang van xoa session cookie cuc bo`() = runTest {
        coEvery { authApiService.logout() } throws IOException("timeout")
        coEvery { sessionCookieStore.clear() } returns Unit

        repository.logout()

        coVerify(exactly = 1) { sessionCookieStore.clear() }
    }

    @Test
    fun `hasActiveSession thi doc tu SessionCookieStore`() = runTest {
        coEvery { sessionCookieStore.hasActiveSessionCookie() } returns true

        val result = repository.hasActiveSession()

        assertTrue(result)
    }
}
