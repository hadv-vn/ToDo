package com.example.todo.domain.usecase.auth

/**
 * Rule validate dùng chung giữa các UseCase Auth (và tái dùng ở Presentation để tránh lặp regex
 * — cùng 1 Gradle module nên `internal` vẫn truy cập được từ `presentation.auth`, xem LLD mục
 * 3.1 "ranh giới layer giữ qua convention import"). Rule email/password khớp auth-core
 * `RegisterDto`/`ResetPasswordDto` (xem `password-rules.ts`). Token reset-password không có
 * format cố định (opaque string từ email) — validate "không rỗng" trực tiếp ở
 * `ResetPasswordUseCase`, không cần rule riêng ở đây.
 */
internal object AuthValidationRules {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // minLength 8, maxLength 72 (giới hạn bcrypt), >=1 chữ + >=1 số — API_Contract mục 2.1.
    private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isValidPassword(password: String): Boolean = PASSWORD_REGEX.matches(password)
}
