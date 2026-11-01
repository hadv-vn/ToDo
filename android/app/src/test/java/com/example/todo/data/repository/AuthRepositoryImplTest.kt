package com.example.todo.data.repository

import com.example.todo.data.local.TokenLocalDataSource
import com.example.todo.data.remote.api.AuthApiService
import com.example.todo.data.remote.dto.auth.AuthResponseDto
import com.example.todo.data.remote.dto.auth.LoginRequestDto
import com.example.todo.data.remote.dto.auth.RegisterRequestDto
import com.example.todo.data.remote.dto.auth.UserDto
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
 * Unit test cho [AuthRepositoryImpl] — MockK cho [AuthApiService]/[TokenLocalDataSource], không
 * gọi network thật. Theo LLD mục 3.5: register/login thành công -> lưu token qua
 * [TokenLocalDataSource.saveToken] TRƯỚC khi trả [ApiResult.Success]; lỗi mạng/HTTP -> KHÔNG lưu
 * token, map đúng qua [com.example.todo.data.remote.safeApiCall].
 */
class AuthRepositoryImplTest {

    private val authApiService: AuthApiService = mockk()
    private val tokenLocalDataSource: TokenLocalDataSource = mockk()
    private val repository = AuthRepositoryImpl(authApiService, tokenLocalDataSource)

    @Test
    fun `register thanh cong thi luu token va tra AuthSession dung du lieu`() = runTest {
        val responseDto = AuthResponseDto(
            accessToken = "token-abc",
            user = UserDto(id = "u1", email = "a@b.com", createdAt = "2026-10-14T00:00:00.000Z")
        )
        coEvery { authApiService.register(any()) } returns Response.success(responseDto)
        coEvery { tokenLocalDataSource.saveToken("token-abc") } returns Unit

        val result = repository.register("a@b.com", "password1", "password1")

        val expectedSession = AuthSession(accessToken = "token-abc", user = AuthUser(id = "u1", email = "a@b.com"))
        assertEquals(ApiResult.Success(expectedSession), result)
        coVerify(exactly = 1) { tokenLocalDataSource.saveToken("token-abc") }
    }

    @Test
    fun `login thanh cong thi luu token va tra AuthSession dung du lieu`() = runTest {
        val responseDto = AuthResponseDto(
            accessToken = "token-xyz",
            user = UserDto(id = "u2", email = "c@d.com", createdAt = "2026-10-14T00:00:00.000Z")
        )
        coEvery { authApiService.login(any()) } returns Response.success(responseDto)
        coEvery { tokenLocalDataSource.saveToken("token-xyz") } returns Unit

        val result = repository.login("c@d.com", "password1")

        val expectedSession = AuthSession(accessToken = "token-xyz", user = AuthUser(id = "u2", email = "c@d.com"))
        assertEquals(ApiResult.Success(expectedSession), result)
        coVerify(exactly = 1) { tokenLocalDataSource.saveToken("token-xyz") }
    }

    @Test
    fun `register mat mang thi tra NetworkError va khong luu token`() = runTest {
        coEvery { authApiService.register(any()) } throws IOException("timeout")

        val result = repository.register("a@b.com", "password1", "password1")

        assertEquals(ApiResult.NetworkError, result)
        coVerify(exactly = 0) { tokenLocalDataSource.saveToken(any()) }
    }

    @Test
    fun `login sai mat khau 401 thi tra Error UNAUTHORIZED va khong luu token`() = runTest {
        val errorBody = "{\"message\":\"Email hoặc mật khẩu không đúng\"}"
            .toResponseBody("application/json".toMediaType())
        coEvery { authApiService.login(any()) } returns Response.error(401, errorBody)

        val result = repository.login("a@b.com", "wrongpass")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ApiErrorCode.UNAUTHORIZED, error.code)
        assertEquals("Email hoặc mật khẩu không đúng", error.message)
        coVerify(exactly = 0) { tokenLocalDataSource.saveToken(any()) }
    }

    @Test
    fun `logout thi chi xoa token cuc bo`() = runTest {
        coEvery { tokenLocalDataSource.clearToken() } returns Unit

        repository.logout()

        coVerify(exactly = 1) { tokenLocalDataSource.clearToken() }
    }

    @Test
    fun `hasStoredToken thi doc tu TokenLocalDataSource`() = runTest {
        coEvery { tokenLocalDataSource.hasToken() } returns true

        val result = repository.hasStoredToken()

        assertTrue(result)
    }
}
