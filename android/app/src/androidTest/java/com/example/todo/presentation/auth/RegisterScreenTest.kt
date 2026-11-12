package com.example.todo.presentation.auth

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.AuthRepository
import com.example.todo.domain.usecase.auth.RegisterUseCase
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test cho luồng Đăng ký (LLD mục 6.1, FR-01/SRS) — nhập email/password/confirm, nhấn
 * "Đăng ký", xác nhận validate confirm-password inline KHÔNG gọi API, và luồng thành công khóa
 * form + hiện `successMessage` (auth-core bắt buộc xác thực email trước khi login được nên đăng ký
 * thành công KHÔNG tự động vào app — xem doc [RegisterScreen]).
 *
 * Cùng kỹ thuật [FakeAuthRepository] viết tay như LoginScreenTest — không dùng MockK vì module
 * chỉ khai báo `mockk` cho `testImplementation`, không có `mockk-android` cho
 * `androidTestImplementation`.
 */
class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeAuthRepository(
        private val pendingRegisterResult: CompletableDeferred<ApiResult<Unit>>
    ) : AuthRepository {
        override suspend fun register(email: String, password: String): ApiResult<Unit> =
            pendingRegisterResult.await()

        override suspend fun login(email: String, password: String) =
            throw NotImplementedError("Không dùng trong RegisterScreenTest")

        override suspend fun logout() = Unit

        override suspend fun resendVerification(email: String) =
            throw NotImplementedError("Không dùng trong RegisterScreenTest")

        override suspend fun forgotPassword(email: String) =
            throw NotImplementedError("Không dùng trong RegisterScreenTest")

        override suspend fun resetPassword(token: String, newPassword: String) =
            throw NotImplementedError("Không dùng trong RegisterScreenTest")

        override suspend fun hasActiveSession(): Boolean = false
    }

    @Test
    fun xacNhanMatKhauKhongKhop_hienLoiInline_khongGoiApi() {
        // pendingRegisterResult KHÔNG bao giờ complete -- nếu validate client không chặn mà vẫn gọi
        // tới repository, coroutine sẽ treo mãi và "Đăng ký" sẽ không biến mất khỏi nút (nút vẫn
        // disable ở trạng thái loading) -> assertIsNotEnabled bên dưới sẽ fail nếu rơi vào nhánh đó.
        val viewModel = RegisterViewModel(RegisterUseCase(FakeAuthRepository(CompletableDeferred())))

        composeTestRule.setContent {
            RegisterScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Email").performTextInput("a@b.com")
        composeTestRule.onNodeWithText("Mật khẩu").performTextInput("password1")
        composeTestRule.onNodeWithText("Xác nhận mật khẩu").performTextInput("khackhau1")

        // "Đăng ký" xuất hiện 2 lần: tiêu đề màn hình + nhãn nút (giống LoginScreenTest).
        composeTestRule.onAllNodesWithText("Đăng ký")[1].performClick()

        composeTestRule.onNodeWithText("Xác nhận mật khẩu không khớp").assertExists()
        // Nút vẫn ở trạng thái enable, không rơi vào loading (không gọi API).
        composeTestRule.onAllNodesWithText("Đăng ký").assertCountEquals(2)
    }

    @Test
    fun nhapDuLieuHopLe_dangKyThanhCong_hienThongBaoVaKhoaForm() {
        val pending = CompletableDeferred<ApiResult<Unit>>()
        val viewModel = RegisterViewModel(RegisterUseCase(FakeAuthRepository(pending)))

        composeTestRule.setContent {
            RegisterScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Email").performTextInput("a@b.com")
        composeTestRule.onNodeWithText("Mật khẩu").performTextInput("password1")
        composeTestRule.onNodeWithText("Xác nhận mật khẩu").performTextInput("password1")

        composeTestRule.onAllNodesWithText("Đăng ký")[1].performClick()

        // Đang submit -> nhãn nút đổi sang CircularProgressIndicator -> chỉ còn 1 node "Đăng ký"
        // (tiêu đề), giống LoginScreenTest.
        composeTestRule.onAllNodesWithText("Đăng ký").assertCountEquals(1)

        pending.complete(ApiResult.Success(Unit))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản trước khi đăng nhập."
        ).assertExists()
        // registerSuccess=true -> fieldsEnabled=false (LLD: không cho sửa lại form sau thành công).
        composeTestRule.onNodeWithText("Email").assertIsNotEnabled()
    }
}
