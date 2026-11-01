package com.example.todo.domain.common

/**
 * Result type xuyên suốt Repository -> UseCase -> ViewModel — theo LLD mục 3.3.
 *
 * Đặt trong domain/common (Kotlin thuần, không phụ thuộc Android) để cả Domain lẫn Data đều
 * dùng được. Đây chính là cơ chế đảm bảo exception không bao giờ bay tự do lên UI: mọi lỗi
 * (HTTP 4xx/5xx, IOException mất mạng, lỗi parse JSON) đều được Repository Impl chặn và chuyển
 * thành một trong 3 nhánh dưới đây trước khi trả lên UseCase/ViewModel (xem safeApiCall ở LLD mục 3.6).
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: ApiErrorCode, val message: String?) : ApiResult<Nothing>()
    data object NetworkError : ApiResult<Nothing>()
}
