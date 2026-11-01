package com.example.todo.domain.usecase.auth

/**
 * Rule validate dùng chung giữa các UseCase Auth (và tái dùng ở Presentation để tránh lặp regex
 * — cùng 1 Gradle module nên `internal` vẫn truy cập được từ `presentation.auth`, xem LLD mục
 * 3.1 "ranh giới layer giữ qua convention import"). Rule khớp API_Contract_ToDo_Android.md
 * mục 2.1/2.4.
 */
internal object AuthValidationRules {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // minLength 8, maxLength 72 (giới hạn bcrypt), >=1 chữ + >=1 số — API_Contract mục 2.1.
    private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$")

    // Đúng 6 ký tự số — API_Contract mục 2.4.
    private val OTP_REGEX = Regex("^\\d{6}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isValidPassword(password: String): Boolean = PASSWORD_REGEX.matches(password)

    fun isValidOtp(otp: String): Boolean = OTP_REGEX.matches(otp)
}
