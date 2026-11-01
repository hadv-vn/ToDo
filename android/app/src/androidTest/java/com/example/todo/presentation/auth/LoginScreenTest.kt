package com.example.todo.presentation.auth

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.model.AuthUser
import com.example.todo.domain.repository.AuthRepository
import com.example.todo.domain.usecase.auth.LoginUseCase
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test cho luồng Đăng nhập (LLD mục 6.1/8) — nhập email/password, nhấn "Đăng nhập",
 * xác nhận trạng thái loading hiển thị đúng trong lúc chờ kết quả.
 *
 * Không dùng MockK ở đây (module chỉ khai báo `mockk` cho `testImplementation`, KHÔNG có
 * `mockk-android` cho `androidTestImplementation` — xem báo cáo mục "Code khó test"/ghi chú môi
 * trường). Thay vào đó dùng [FakeAuthRepository] viết tay implement thẳng interface
 * [AuthRepository], rồi dựng [LoginUseCase]/[LoginViewModel] THẬT — [LoginScreen] nhận `viewModel`
 * qua tham số có default `hiltViewModel()` nên test truyền thẳng instance thật vào, KHÔNG cần Hilt.
 *
 * LƯU Ý QUAN TRỌNG: máy phát triển hiện tại KHÔNG có Android SDK/emulator, nên
 * `./gradlew :app:connectedAndroidTest` CHƯA thể chạy thật được. Test này đã được rà soát thủ công
 * kỹ nhưng CHƯA verify bằng cách chạy thật.
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeAuthRepository(
        private val pendingLoginResult: CompletableDeferred<ApiResult<AuthSession>>
    ) : AuthRepository {
        override suspend fun register(email: String, password: String, confirmPassword: String) =
            throw NotImplementedError("Không dùng trong LoginScreenTest")

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            pendingLoginResult.await()

        override suspend fun logout() = Unit

        override suspend fun forgotPassword(email: String) =
            throw NotImplementedError("Không dùng trong LoginScreenTest")

        override suspend fun resetPassword(
            email: String,
            otp: String,
            newPassword: String,
            confirmNewPassword: String
        ) = throw NotImplementedError("Không dùng trong LoginScreenTest")

        override suspend fun hasStoredToken(): Boolean = false
    }

    @Test
    fun nhapEmailPasswordNhanDangNhap_hienThiLoadingDungLucChoKetQua() {
        val pending = CompletableDeferred<ApiResult<AuthSession>>()
        val viewModel = LoginViewModel(LoginUseCase(FakeAuthRepository(pending)))
        var navigatedToTaskList = false

        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToTaskList = { navigatedToTaskList = true }
            )
        }

        // "Đăng nhập" xuất hiện 2 lần lúc này: tiêu đề màn hình (Text headlineMedium) + nhãn nút.
        composeTestRule.onAllNodesWithText("Đăng nhập").assertCountEquals(2)

        composeTestRule.onNodeWithText("Email").performTextInput("a@b.com")
        composeTestRule.onNodeWithText("Mật khẩu").performTextInput("password1")

        // Nút nằm ở vị trí thứ 2 trong số các node có text "Đăng nhập" (sau tiêu đề, theo thứ tự
        // composition của Column) — cách duy nhất phân biệt 2 node trùng text khi không có testTag
        // ở LoginScreen hiện tại.
        composeTestRule.onAllNodesWithText("Đăng nhập")[1].performClick()

        // Khi isSubmitting=true, nội dung Button đổi từ Text("Đăng nhập") sang CircularProgressIndicator
        // -> chỉ còn 1 node khớp text "Đăng nhập" (tiêu đề màn hình).
        composeTestRule.onAllNodesWithText("Đăng nhập").assertCountEquals(1)

        pending.complete(
            ApiResult.Success(AuthSession(accessToken = "token", user = AuthUser(id = "u1", email = "a@b.com")))
        )
        composeTestRule.waitForIdle()

        assert(navigatedToTaskList) { "onNavigateToTaskList phải được gọi sau khi login thành công" }
    }
}
