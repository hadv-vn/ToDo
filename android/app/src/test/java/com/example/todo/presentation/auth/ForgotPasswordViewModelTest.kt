package com.example.todo.presentation.auth

import app.cash.turbine.test
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.usecase.auth.ForgotPasswordUseCase
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
 * Unit test cho [ForgotPasswordViewModel] — MockK cho [ForgotPasswordUseCase], Turbine cho StateFlow.
 * Theo LLD mục 4.2/6.2.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val forgotPasswordUseCase: ForgotPasswordUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSubmit email sai dinh dang thi khong goi UseCase va hien loi inline`() = runTest(testDispatcher) {
        val viewModel = ForgotPasswordViewModel(forgotPasswordUseCase)
        viewModel.onEmailChange("khong-phai-email")

        viewModel.onSubmit()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Email không đúng định dạng", state.emailError)
            assertFalse(state.requestSent)
        }
        coVerify(exactly = 0) { forgotPasswordUseCase(any()) }
    }

    @Test
    fun `onSubmit email hop le thi goi UseCase va set requestSent true`() = runTest(testDispatcher) {
        coEvery { forgotPasswordUseCase("a@b.com") } returns ApiResult.Success(Unit)

        val viewModel = ForgotPasswordViewModel(forgotPasswordUseCase)
        viewModel.onEmailChange("a@b.com")

        viewModel.uiState.test {
            awaitItem() // sau onEmailChange

            viewModel.onSubmit()

            val submitting = awaitItem()
            assertTrue(submitting.isSubmitting)

            val sent = awaitItem()
            assertTrue(sent.requestSent)
            assertFalse(sent.isSubmitting)
        }
        coVerify(exactly = 1) { forgotPasswordUseCase("a@b.com") }
    }

    @Test
    fun `onSubmit loi mang thi requestSent van la false va hien loi`() = runTest(testDispatcher) {
        coEvery { forgotPasswordUseCase("a@b.com") } returns ApiResult.NetworkError

        val viewModel = ForgotPasswordViewModel(forgotPasswordUseCase)
        viewModel.onEmailChange("a@b.com")

        viewModel.uiState.test {
            awaitItem()

            viewModel.onSubmit()

            awaitItem() // isSubmitting = true
            val errorState = awaitItem()
            assertFalse(errorState.requestSent) // CHỈ true khi backend thực sự phản hồi 200
            assertEquals("Không có kết nối mạng, vui lòng thử lại", errorState.generalError)
        }
    }
}
