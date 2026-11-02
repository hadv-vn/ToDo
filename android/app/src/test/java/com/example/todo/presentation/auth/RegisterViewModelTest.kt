package com.example.todo.presentation.auth

import app.cash.turbine.test
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.auth.RegisterUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
 * Unit test cho [RegisterViewModel] — MockK cho [RegisterUseCase], Turbine cho StateFlow.
 * Theo LLD mục 4.2/6.1 (validate inline đồng bộ TRƯỚC khi gọi UseCase).
 *
 * auth-core bắt buộc xác thực email trước khi login -> `registerUseCase` trả `ApiResult<Unit>`
 * (KHÔNG còn `AuthSession`), đăng ký thành công chỉ khóa form + hiện `successMessage`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val registerUseCase: RegisterUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm(viewModel: RegisterViewModel) {
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("password1")
        viewModel.onConfirmPasswordChange("password1")
    }

    @Test
    fun `onSubmit email sai dinh dang thi khong goi UseCase va hien loi inline`() = runTest(testDispatcher) {
        val viewModel = RegisterViewModel(registerUseCase)
        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange("password1")
        viewModel.onConfirmPasswordChange("password1")

        viewModel.onSubmit()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Email không đúng định dạng", state.emailError)
            assertFalse(state.isSubmitting)
            assertFalse(state.registerSuccess)
        }
        coVerify(exactly = 0) { registerUseCase(any(), any(), any()) }
    }

    @Test
    fun `onSubmit mat khau yeu thi khong goi UseCase va hien loi inline`() = runTest(testDispatcher) {
        val viewModel = RegisterViewModel(registerUseCase)
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("short1") // 6 ký tự -> chưa đủ 8
        viewModel.onConfirmPasswordChange("short1")

        viewModel.onSubmit()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(
                "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 chữ và 1 số",
                state.passwordError
            )
        }
        coVerify(exactly = 0) { registerUseCase(any(), any(), any()) }
    }

    @Test
    fun `onSubmit confirmPassword khong khop thi khong goi UseCase va hien loi inline`() = runTest(testDispatcher) {
        val viewModel = RegisterViewModel(registerUseCase)
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("password1")
        viewModel.onConfirmPasswordChange("password2")

        viewModel.onSubmit()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Xác nhận mật khẩu không khớp", state.confirmPasswordError)
        }
        coVerify(exactly = 0) { registerUseCase(any(), any(), any()) }
    }

    @Test
    fun `onSubmit thanh cong thi set registerSuccess true, hien successMessage, tat isSubmitting`() =
        runTest(testDispatcher) {
            coEvery { registerUseCase("a@b.com", "password1", "password1") } returns ApiResult.Success(Unit)

            val viewModel = RegisterViewModel(registerUseCase)
            fillValidForm(viewModel)

            viewModel.uiState.test {
                awaitItem() // state hiện tại sau 3 lần onXxxChange, trước khi submit

                viewModel.onSubmit()

                val submitting = awaitItem()
                assertTrue(submitting.isSubmitting)

                val success = awaitItem()
                assertTrue(success.registerSuccess)
                assertFalse(success.isSubmitting)
                assertTrue(success.successMessage?.isNotBlank() == true)
            }
        }

    @Test
    fun `onSubmit email da ton tai thi hien loi Email da duoc su dung`() = runTest(testDispatcher) {
        coEvery { registerUseCase("a@b.com", "password1", "password1") } returns
            ApiResult.Error(ApiErrorCode.CONFLICT, "Email đã được sử dụng")

        val viewModel = RegisterViewModel(registerUseCase)
        fillValidForm(viewModel)

        viewModel.uiState.test {
            awaitItem()

            viewModel.onSubmit()

            awaitItem() // isSubmitting = true
            val errorState = awaitItem()
            assertEquals("Email đã được sử dụng", errorState.generalError)
            assertFalse(errorState.isSubmitting)
            assertFalse(errorState.registerSuccess)
        }
    }

    @Test
    fun `goi onSubmit 2 lan lien tiep khi dang isSubmitting thi chi goi UseCase 1 lan`() = runTest(testDispatcher) {
        val pending = CompletableDeferred<ApiResult<Unit>>()
        coEvery { registerUseCase("a@b.com", "password1", "password1") } coAnswers { pending.await() }

        val viewModel = RegisterViewModel(registerUseCase)
        fillValidForm(viewModel)

        viewModel.onSubmit() // lần 1 -> isSubmitting=true, coroutine suspend ở pending.await()
        viewModel.onSubmit() // lần 2 ngay sau đó -> bị chặn bởi "if (state.isSubmitting) return"

        coVerify(exactly = 1) { registerUseCase("a@b.com", "password1", "password1") }

        pending.complete(ApiResult.Success(Unit))
    }
}
