package com.example.todo.presentation.auth

import app.cash.turbine.test
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.AuthSession
import com.example.todo.domain.model.AuthUser
import com.example.todo.domain.usecase.auth.LoginUseCase
import com.example.todo.domain.usecase.auth.ResendVerificationUseCase
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
 * Unit test cho [LoginViewModel] — MockK cho [LoginUseCase]/[ResendVerificationUseCase], Turbine
 * cho StateFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val loginUseCase: LoginUseCase = mockk()
    private val resendVerificationUseCase: ResendVerificationUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = LoginViewModel(loginUseCase, resendVerificationUseCase)

    @Test
    fun `onSubmit voi email rong thi khong goi UseCase va hien loi inline`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onPasswordChange("abc12345")
        viewModel.onSubmit()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Email không được để trống", state.emailError)
            assertFalse(state.isSubmitting)
            assertFalse(state.loginSuccess)
        }
        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }

    @Test
    fun `onSubmit thanh cong thi set loginSuccess true va tat loading`() = runTest(testDispatcher) {
        val session = AuthSession(user = AuthUser(id = "u1", email = "a@b.com"))
        coEvery { loginUseCase("a@b.com", "password1") } returns ApiResult.Success(session)

        val viewModel = createViewModel()
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("password1")

        viewModel.uiState.test {
            awaitItem() // state hiện tại sau 2 lần onXxxChange, trước khi submit

            viewModel.onSubmit()

            val submitting = awaitItem()
            assertTrue(submitting.isSubmitting)

            val success = awaitItem()
            assertTrue(success.loginSuccess)
            assertFalse(success.isSubmitting)
        }
    }

    @Test
    fun `onSubmit sai mat khau thi hien loi chung khong tiet lo truong nao sai`() = runTest(testDispatcher) {
        coEvery { loginUseCase("a@b.com", "wrongpass") } returns
            ApiResult.Error(ApiErrorCode.UNAUTHORIZED, "Email hoặc mật khẩu không đúng")

        val viewModel = createViewModel()
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("wrongpass")

        viewModel.uiState.test {
            awaitItem()

            viewModel.onSubmit()

            awaitItem() // isSubmitting = true
            val errorState = awaitItem()
            assertEquals("Email hoặc mật khẩu không đúng", errorState.generalError)
            assertFalse(errorState.isSubmitting)
            assertFalse(errorState.loginSuccess)
            assertFalse(errorState.showResendVerification)
        }
    }

    @Test
    fun `onSubmit email chua xac thuc thi hien nut gui lai email xac thuc`() = runTest(testDispatcher) {
        coEvery { loginUseCase("a@b.com", "password1") } returns
            ApiResult.Error(ApiErrorCode.FORBIDDEN, "Email chưa được xác thực", "EMAIL_NOT_VERIFIED")

        val viewModel = createViewModel()
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("password1")

        viewModel.uiState.test {
            awaitItem()

            viewModel.onSubmit()

            awaitItem() // isSubmitting = true
            val errorState = awaitItem()
            assertEquals("Email chưa được xác thực", errorState.generalError)
            assertTrue(errorState.showResendVerification)
        }
    }

    @Test
    fun `onResendVerification thanh cong thi hien infoMessage va tat nut resend`() = runTest(testDispatcher) {
        coEvery { loginUseCase("a@b.com", "password1") } returns
            ApiResult.Error(ApiErrorCode.FORBIDDEN, "Email chưa được xác thực", "EMAIL_NOT_VERIFIED")
        coEvery { resendVerificationUseCase("a@b.com") } returns ApiResult.Success(Unit)

        val viewModel = createViewModel()
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("password1")
        viewModel.onSubmit()

        viewModel.uiState.test {
            awaitItem() // state sau login lỗi EMAIL_NOT_VERIFIED

            viewModel.onResendVerification()

            awaitItem() // isSubmitting = true
            val state = awaitItem()
            assertFalse(state.showResendVerification)
            assertEquals(
                "Nếu email tồn tại và chưa xác thực, một email xác thực mới đã được gửi.",
                state.infoMessage
            )
        }
        coVerify(exactly = 1) { resendVerificationUseCase("a@b.com") }
    }
}
