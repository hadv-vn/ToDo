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

    /**
     * `errorCode` — mã lỗi nghiệp vụ ổn định do backend trả (`error.code`, ví dụ
     * `"EMAIL_NOT_VERIFIED"`, xem auth-core `AllExceptionsFilter`) — dùng khi UI cần phân biệt
     * lỗi CHI TIẾT HƠN [code] (chỉ suy từ HTTP status, không đủ phân biệt vd 403 do sai quyền hay
     * do task thuộc user khác). `null` nếu response lỗi không theo envelope `{error:{code,...}}`
     * (vd lỗi mạng tầng dưới hiếm gặp map nhầm vào đây, hoặc lỗi không rõ nguồn).
     */
    data class Error(
        val code: ApiErrorCode,
        val message: String?,
        val errorCode: String? = null
    ) : ApiResult<Nothing>()

    data object NetworkError : ApiResult<Nothing>()
}
