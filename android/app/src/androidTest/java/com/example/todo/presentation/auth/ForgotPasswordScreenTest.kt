package com.example.todo.presentation.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.AuthRepository
import com.example.todo.domain.usecase.auth.ForgotPasswordUseCase
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test cho luồng Quên mật khẩu (LLD mục 6.2, FR-05/SRS) — nhập email, xác nhận validate
 * format inline KHÔNG gọi API, và luồng thành công tự điều hướng sang ResetPasswordScreen kèm đúng
 * email đã nhập (xem [ForgotPasswordScreen] — backend luôn trả 200 để chống user-enumeration nên
 * app không phân biệt email tồn tại hay không).
 */
class ForgotPasswordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeAuthRepository(
        private val pendingForgotPasswordResult: CompletableDeferred<ApiResult<Unit>>
    ) : AuthRepository {
        override suspend fun register(email: String, password: String) =
            throw NotImplementedError("Không dùng trong ForgotPasswordScreenTest")

        override suspend fun login(email: String, password: String) =
            throw NotImplementedError("Không dùng trong ForgotPasswordScreenTest")

        override suspend fun logout() = Unit

        override suspend fun resendVerification(email: String) =
            throw NotImplementedError("Không dùng trong ForgotPasswordScreenTest")

        override suspend fun forgotPassword(email: String): ApiResult<Unit> =
            pendingForgotPasswordResult.await()

        override suspend fun resetPassword(token: String, newPassword: String) =
            throw NotImplementedError("Không dùng trong ForgotPasswordScreenTest")

        override suspend fun hasActiveSession(): Boolean = false
    }

    @Test
    fun emailSaiDinhDang_hienLoiInline_khongGoiApiKhongDieuHuong() {
        // pendingForgotPasswordResult không complete -- nếu validate không chặn, requestSent sẽ
        // không bao giờ lên true nên onNavigateToResetPassword chắc chắn không bị gọi nhầm.
        val viewModel = ForgotPasswordViewModel(
            ForgotPasswordUseCase(FakeAuthRepository(CompletableDeferred()))
        )
        var navigated = false

        composeTestRule.setContent {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToResetPassword = { navigated = true }
            )
        }

        composeTestRule.onNodeWithText("Email").performTextInput("khong-hop-le")
        composeTestRule.onNodeWithText("Gửi token xác nhận").performClick()

        composeTestRule.onNodeWithText("Email không đúng định dạng").assertExists()
        assert(!navigated) { "KHÔNG được điều hướng khi email sai định dạng" }
    }

    @Test
    fun emailHopLe_guiThanhCong_dieuHuongSangResetPasswordKemDungEmail() {
        val pending = CompletableDeferred<ApiResult<Unit>>()
        val viewModel = ForgotPasswordViewModel(ForgotPasswordUseCase(FakeAuthRepository(pending)))
        var navigatedEmail: String? = null

        composeTestRule.setContent {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToResetPassword = { email -> navigatedEmail = email }
            )
        }

        composeTestRule.onNodeWithText("Email").performTextInput("a@b.com")
        composeTestRule.onNodeWithText("Gửi token xác nhận").performClick()

        pending.complete(ApiResult.Success(Unit))
        composeTestRule.waitForIdle()

        assert(navigatedEmail == "a@b.com") {
            "Phải điều hướng sang ResetPasswordScreen kèm đúng email đã nhập, thực tế: $navigatedEmail"
        }
    }
}
