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
            val parsedError = parseErrorBody(response.errorBody())
            ApiResult.Error(code, parsedError?.message, parsedError?.code)
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

// Envelope lỗi chuẩn của auth-core — `{ error: { code, message, details?, retryAfterSeconds? } }`,
// xem auth-core/backend/src/common/filters/all-exceptions.filter.ts. Chỉ cần `code`/`message` ở
// Android (details/retryAfterSeconds chưa dùng tới).
@Serializable
private data class ErrorEnvelopeDto(val error: ErrorBodyDto? = null)

@Serializable
private data class ErrorBodyDto(val code: String? = null, val message: String? = null)

private data class ParsedError(val code: String?, val message: String?)

private val errorBodyJson = Json { ignoreUnknownKeys = true }

private fun parseErrorBody(errorBody: ResponseBody?): ParsedError? {
    return try {
        val raw = errorBody?.string() ?: return null
        val body = errorBodyJson.decodeFromString<ErrorEnvelopeDto>(raw).error ?: return null
        ParsedError(code = body.code, message = body.message)
    } catch (e: Exception) {
        null
    }
}
