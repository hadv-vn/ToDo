package com.example.todo.data.remote

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Unit test trực tiếp cho [safeApiCall] — theo LLD mục 3.6: chặn exception bay lên UI, map
 * `Response<T>`/exception -> [ApiResult] đúng từng nhánh (HTTP status code, IOException, lỗi
 * không lường trước).
 */
@Serializable
private data class FakeBody(val value: String)

class SafeApiCallTest {

    // Envelope lỗi chuẩn của auth-core — `{ error: { code, message } }`, xem
    // auth-core/backend/src/common/filters/all-exceptions.filter.ts.
    private fun jsonError(message: String, code: String = "SOME_ERROR_CODE"): ResponseBody =
        "{\"error\":{\"code\":\"$code\",\"message\":\"$message\"}}".toResponseBody("application/json".toMediaType())

    @Test
    fun `response thanh cong thi tra Success voi dung data`() = runTest {
        val result = safeApiCall { Response.success(FakeBody("abc")) }

        assertEquals(ApiResult.Success(FakeBody("abc")), result)
    }

    @Test
    fun `status 400 thi map thanh Error BAD_REQUEST, giu dung errorCode tu server`() = runTest {
        val result = safeApiCall<FakeBody> {
            Response.error(400, jsonError("Dữ liệu không hợp lệ", code = "VALIDATION_ERROR"))
        }

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ApiErrorCode.BAD_REQUEST, error.code)
        assertEquals("Dữ liệu không hợp lệ", error.message)
        assertEquals("VALIDATION_ERROR", error.errorCode)
    }

    @Test
    fun `status 401 thi map thanh Error UNAUTHORIZED`() = runTest {
        val result = safeApiCall<FakeBody> { Response.error(401, jsonError("Chưa đăng nhập")) }

        assertEquals(ApiErrorCode.UNAUTHORIZED, (result as ApiResult.Error).code)
    }

    @Test
    fun `status 403 thi map thanh Error FORBIDDEN`() = runTest {
        val result = safeApiCall<FakeBody> { Response.error(403, jsonError("Không có quyền")) }

        assertEquals(ApiErrorCode.FORBIDDEN, (result as ApiResult.Error).code)
    }

    @Test
    fun `status 404 thi map thanh Error NOT_FOUND`() = runTest {
        val result = safeApiCall<FakeBody> { Response.error(404, jsonError("Không tìm thấy")) }

        assertEquals(ApiErrorCode.NOT_FOUND, (result as ApiResult.Error).code)
    }

    @Test
    fun `status 409 thi map thanh Error CONFLICT`() = runTest {
        val result = safeApiCall<FakeBody> { Response.error(409, jsonError("Email đã tồn tại")) }

        assertEquals(ApiErrorCode.CONFLICT, (result as ApiResult.Error).code)
    }

    @Test
    fun `status 500 thi map thanh Error SERVER_ERROR`() = runTest {
        val result = safeApiCall<FakeBody> { Response.error(500, jsonError("Lỗi hệ thống")) }

        assertEquals(ApiErrorCode.SERVER_ERROR, (result as ApiResult.Error).code)
    }

    @Test
    fun `status khong xac dinh thi map thanh Error UNKNOWN`() = runTest {
        val result = safeApiCall<FakeBody> { Response.error(418, jsonError("teapot")) }

        assertEquals(ApiErrorCode.UNKNOWN, (result as ApiResult.Error).code)
    }

    @Test
    fun `error body khong phai JSON hop le thi message la null khong crash`() = runTest {
        val malformedBody = "khong-phai-json".toResponseBody("text/plain".toMediaType())
        val result = safeApiCall<FakeBody> { Response.error(400, malformedBody) }

        assertTrue(result is ApiResult.Error)
        assertNull((result as ApiResult.Error).message)
    }

    @Test
    fun `IOException thi tra NetworkError`() = runTest {
        val result = safeApiCall<FakeBody> { throw IOException("Mất kết nối") }

        assertEquals(ApiResult.NetworkError, result)
    }

    @Test
    fun `exception khong luong truoc thi tra Error UNKNOWN voi message cua exception`() = runTest {
        val result = safeApiCall<FakeBody> { throw IllegalStateException("Lỗi không lường trước") }

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ApiErrorCode.UNKNOWN, error.code)
        assertEquals("Lỗi không lường trước", error.message)
    }
}
