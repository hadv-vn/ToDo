package com.example.todo.presentation.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.AuthRepository
import com.example.todo.domain.usecase.auth.ResetPasswordUseCase
import com.example.todo.presentation.navigation.Routes
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test cho luồng Đặt lại mật khẩu (LLD mục 6.2, FR-05/SRS) — email hiển thị lấy từ nav
 * arg qua [SavedStateHandle] (giống cách thật [ResetPasswordViewModel] đọc `Routes.RESET_PASSWORD_ARG_EMAIL`
 * khi Hilt Navigation Compose gắn argument của route `reset_password/{email}`), validate token
 * rỗng inline KHÔNG gọi API, và luồng thành công tự điều hướng về Đăng nhập.
 */
class ResetPasswordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeAuthRepository(
        private val pendingResetPasswordResult: CompletableDeferred<ApiResult<Unit>>
    ) : AuthRepository {
        override suspend fun register(email: String, password: String) =
            throw NotImplementedError("Không dùng trong ResetPasswordScreenTest")

        override suspend fun login(email: String, password: String) =
            throw NotImplementedError("Không dùng trong ResetPasswordScreenTest")

        override suspend fun logout() = Unit

        override suspend fun resendVerification(email: String) =
            throw NotImplementedError("Không dùng trong ResetPasswordScreenTest")

        override suspend fun forgotPassword(email: String) =
            throw NotImplementedError("Không dùng trong ResetPasswordScreenTest")

        override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> =
            pendingResetPasswordResult.await()

        override suspend fun hasActiveSession(): Boolean = false
    }

    private fun createViewModel(
        pendingResetPasswordResult: CompletableDeferred<ApiResult<Unit>>,
        email: String = "a@b.com"
    ): ResetPasswordViewModel = ResetPasswordViewModel(
        resetPasswordUseCase = ResetPasswordUseCase(FakeAuthRepository(pendingResetPasswordResult)),
        savedStateHandle = SavedStateHandle(mapOf(Routes.RESET_PASSWORD_ARG_EMAIL to email))
    )

    @Test
    fun hienThiDungEmailLayTuNavArg() {
        composeTestRule.setContent {
            ResetPasswordScreen(viewModel = createViewModel(CompletableDeferred(), email = "a@b.com"))
        }

        composeTestRule.onNodeWithText(
            "Một email chứa token đặt lại mật khẩu đã được gửi tới a@b.com"
        ).assertExists()
    }

    @Test
    fun tokenRong_hienLoiInline_khongGoiApi() {
        // pendingResetPasswordResult không complete -- nếu validate không chặn mà vẫn gọi
        // repository, test sẽ treo ở waitForIdle thay vì fail nhanh ở assertion dưới.
        composeTestRule.setContent {
            ResetPasswordScreen(viewModel = createViewModel(CompletableDeferred()))
        }

        composeTestRule.onNodeWithText("Mật khẩu mới").performTextInput("password1")
        composeTestRule.onNodeWithText("Xác nhận mật khẩu mới").performTextInput("password1")
        composeTestRule.onNodeWithText("Xác nhận").performClick()

        composeTestRule.onNodeWithText("Vui lòng nhập token xác nhận từ email").assertExists()
    }

    @Test
    fun duLieuHopLe_datLaiThanhCong_dieuHuongVeDangNhap() {
        val pending = CompletableDeferred<ApiResult<Unit>>()
        var navigated = false

        composeTestRule.setContent {
            ResetPasswordScreen(
                viewModel = createViewModel(pending),
                onNavigateToLogin = { navigated = true }
            )
        }

        composeTestRule.onNodeWithText("Token xác nhận (lấy từ email)").performTextInput("tok-123")
        composeTestRule.onNodeWithText("Mật khẩu mới").performTextInput("password1")
        composeTestRule.onNodeWithText("Xác nhận mật khẩu mới").performTextInput("password1")
        composeTestRule.onNodeWithText("Xác nhận").performClick()

        pending.complete(ApiResult.Success(Unit))
        composeTestRule.waitForIdle()

        assert(navigated) { "Phải điều hướng về Đăng nhập sau khi đặt lại mật khẩu thành công" }
    }
}
