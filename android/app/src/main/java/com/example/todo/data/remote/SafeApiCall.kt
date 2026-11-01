package com.example.todo.data.remote

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

/**
 * Chặn exception bay lên UI, map `Response<T>` (Retrofit) -> [ApiResult] — theo LLD mục 3.6.
 * Dùng chung cho mọi RepositoryImpl (Auth/Task).
 */
suspend fun <T> safeApiCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(response.body() as T)
        } else {
            val code = mapHttpStatusToApiErrorCode(response.code())
            val message = parseErrorMessage(response.errorBody())
            ApiResult.Error(code, message)
        }
    } catch (e: IOException) {
        // Mất mạng, timeout.
        ApiResult.NetworkError
    } catch (e: Exception) {
        // Lỗi parse JSON / lỗi không lường trước.
        ApiResult.Error(ApiErrorCode.UNKNOWN, e.message)
    }
}

private fun mapHttpStatusToApiErrorCode(httpStatus: Int): ApiErrorCode = when (httpStatus) {
    400 -> ApiErrorCode.BAD_REQUEST
    401 -> ApiErrorCode.UNAUTHORIZED
    403 -> ApiErrorCode.FORBIDDEN
    404 -> ApiErrorCode.NOT_FOUND
    409 -> ApiErrorCode.CONFLICT
    in 500..599 -> ApiErrorCode.SERVER_ERROR
    else -> ApiErrorCode.UNKNOWN
}

// Error Response Schema chuẩn dùng chung mọi endpoint — API_Contract_ToDo_Android.md mục 1.5.
// Chỉ cần field `message` ở Android (details/fieldErrors không dùng để hiển thị theo LLD mục 4.4
// — Android map lỗi theo status code sang chuỗi UI tiếng Việt cố định của riêng nó).
@Serializable
private data class ErrorResponseDto(val message: String? = null)

private val errorBodyJson = Json { ignoreUnknownKeys = true }

private fun parseErrorMessage(errorBody: ResponseBody?): String? {
    return try {
        val raw = errorBody?.string() ?: return null
        errorBodyJson.decodeFromString<ErrorResponseDto>(raw).message
    } catch (e: Exception) {
        null
    }
}
