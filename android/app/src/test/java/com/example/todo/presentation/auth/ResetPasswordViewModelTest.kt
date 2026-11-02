package com.example.todo.presentation.auth

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.auth.ResetPasswordUseCase
import com.example.todo.presentation.navigation.Routes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho [ResetPasswordViewModel] — MockK cho [ResetPasswordUseCase], Turbine cho StateFlow.
 * Theo LLD mục 4.2/6.2. `email` được nhận qua [SavedStateHandle] (nav argument, chỉ hiển thị).
 *
 * auth-core reset-password dùng `token` (lấy từ link email) thay cho `otp` 6 số của backend
 * Express cũ — không có rule format cố định, chỉ validate "không rỗng".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val resetPasswordUseCase: ResetPasswordUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun savedStateHandleWithEmail(email: String) =
        SavedStateHandle(mapOf(Routes.RESET_PASSWORD_ARG_EMAIL to email))

    @Test
    fun `init nhan dung email tu SavedStateHandle`() {
        val viewModel = ResetPasswordViewModel(resetPasswordUseCase, savedStateHandleWithEmail("user@test.com"))

        assertEquals("user@test.com", viewModel.uiState.value.email)
    }

    @Test
    fun `onSubmit token rong thi khong goi UseCase va hien loi inline`() = runTest(testDispatcher) {
        val viewModel = ResetPasswordViewModel(resetPasswordUseCase, savedStateHandleWithEmail("user@test.com"))
        viewModel.onNewPasswordChange("password1")
        viewModel.onConfirmNewPasswordChange("password1")

        viewModel.onSubmit()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Vui lòng nhập token xác nhận từ email", state.tokenError)
            assertFalse(state.resetSuccess)
        }
        coVerify(exactly = 0) { resetPasswordUseCase(any(), any(), any()) }
    }

    @Test
    fun `onSubmit token va password hop le thi goi UseCase va set resetSuccess true`() = runTest(testDispatcher) {
        coEvery { resetPasswordUseCase("reset-token-abc", "password1", "password1") } returns
            ApiResult.Success(Unit)

        val viewModel = ResetPasswordViewModel(resetPasswordUseCase, savedStateHandleWithEmail("user@test.com"))
        viewModel.onTokenChange("reset-token-abc")
        viewModel.onNewPasswordChange("password1")
        viewModel.onConfirmNewPasswordChange("password1")

        viewModel.uiState.test {
            awaitItem() // sau 3 lần onXxxChange

            viewModel.onSubmit()

            val submitting = awaitItem()
            assertTrue(submitting.isSubmitting)

            val success = awaitItem()
            assertTrue(success.resetSuccess)
            assertFalse(success.isSubmitting)
        }
    }

    @Test
    fun `onSubmit token sai server tra 400 thi hien loi dung message`() = runTest(testDispatcher) {
        coEvery { resetPasswordUseCase("expired-token", "password1", "password1") } returns
            ApiResult.Error(ApiErrorCode.BAD_REQUEST, "Token không đúng hoặc đã hết hạn")

        val viewModel = ResetPasswordViewModel(resetPasswordUseCase, savedStateHandleWithEmail("user@test.com"))
        viewModel.onTokenChange("expired-token")
        viewModel.onNewPasswordChange("password1")
        viewModel.onConfirmNewPasswordChange("password1")

        viewModel.uiState.test {
            awaitItem()

            viewModel.onSubmit()

            awaitItem() // isSubmitting = true
            val errorState = awaitItem()
            // Message lỗi từ server (BAD_REQUEST) được gán vào `generalError` (không có field riêng
            // cho token ở nhánh lỗi server) — khác với lỗi validate client (tokenError) ở trên.
            assertEquals("Token không đúng hoặc đã hết hạn", errorState.generalError)
            assertFalse(errorState.isSubmitting)
            assertFalse(errorState.resetSuccess)
        }
    }
}
