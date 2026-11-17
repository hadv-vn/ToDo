package com.example.todo.presentation.category

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.model.CategoryAppearance
import com.example.todo.domain.usecase.category.CreateCategoryUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.domain.usecase.category.UpdateCategoryUseCase
import com.example.todo.presentation.navigation.Routes
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
 * Unit test cho [CategoryFormViewModel] — dùng chung cho CreateCategoryScreen (categoryId==null) và
 * EditCategoryScreen (categoryId!=null), cùng khuôn [com.example.todo.presentation.taskform.TaskFormViewModelTest].
 *
 * Edit mode tải qua [GetCategoriesUseCase] rồi tự tìm đúng categoryId trong danh sách (backend không
 * có endpoint lấy 1 category theo id — xem CategoryFormViewModel).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryFormViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getCategoriesUseCase: GetCategoriesUseCase = mockk()
    private val createCategoryUseCase: CreateCategoryUseCase = mockk()
    private val updateCategoryUseCase: UpdateCategoryUseCase = mockk()

    private val existingCategory = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): CategoryFormViewModel {
        return CategoryFormViewModel(
            savedStateHandle,
            getCategoriesUseCase,
            createCategoryUseCase,
            updateCategoryUseCase
        )
    }

    // --- Create mode (categoryId == null) ---

    @Test
    fun `Create mode name rong thi khong goi UseCase va hien loi inline`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onNameChange("   ")

        viewModel.onSubmit()

        assertEquals("Tên danh mục không được để trống", viewModel.uiState.value.nameError)
        coVerify(exactly = 0) { createCategoryUseCase(any(), any(), any()) }
    }

    @Test
    fun `Create mode name hop le thi goi CreateCategoryUseCase voi dung color icon va set isSubmitSuccess true`() =
        runTest(testDispatcher) {
            coEvery { createCategoryUseCase("Công việc", CategoryAppearance.DEFAULT_COLOR, CategoryAppearance.DEFAULT_ICON) } returns
                ApiResult.Success(existingCategory)

            val viewModel = createViewModel()
            viewModel.onNameChange("Công việc")

            viewModel.uiState.test {
                awaitItem() // sau onNameChange

                viewModel.onSubmit()

                val submitting = awaitItem()
                assertTrue(submitting.isSubmitting)

                val success = awaitItem()
                assertTrue(success.isSubmitSuccess)
                assertFalse(success.isSubmitting)
            }
            coVerify(exactly = 1) {
                createCategoryUseCase("Công việc", CategoryAppearance.DEFAULT_COLOR, CategoryAppearance.DEFAULT_ICON)
            }
            coVerify(exactly = 0) { updateCategoryUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `Create mode onColorSelected va onIconSelected cap nhat state, onSubmit gui dung gia tri`() =
        runTest(testDispatcher) {
            coEvery { createCategoryUseCase("Công việc", "#43A047", "🛒") } returns ApiResult.Success(existingCategory)

            val viewModel = createViewModel()
            viewModel.onNameChange("Công việc")
            viewModel.onColorSelected("#43A047")
            viewModel.onIconSelected("🛒")

            assertEquals("#43A047", viewModel.uiState.value.color)
            assertEquals("🛒", viewModel.uiState.value.icon)

            viewModel.onSubmit()

            coVerify(exactly = 1) { createCategoryUseCase("Công việc", "#43A047", "🛒") }
        }

    @Test
    fun `Create mode goi onSubmit 2 lan lien tiep thi chi goi UseCase 1 lan`() = runTest(testDispatcher) {
        val pending = CompletableDeferred<ApiResult<Category>>()
        coEvery { createCategoryUseCase(any(), any(), any()) } coAnswers { pending.await() }

        val viewModel = createViewModel()
        viewModel.onNameChange("Công việc")

        viewModel.onSubmit()
        viewModel.onSubmit()

        coVerify(exactly = 1) { createCategoryUseCase(any(), any(), any()) }
        pending.complete(ApiResult.Success(existingCategory))
    }

    // --- Edit mode (categoryId != null) ---

    @Test
    fun `Edit mode init tim dung category trong danh sach va nap vao state`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(existingCategory))
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_CATEGORY_ARG_CATEGORY_ID to "cat-1"))

        val viewModel = createViewModel(savedStateHandle)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Công việc", state.name)
        assertEquals("#1E88E5", state.color)
        assertEquals("💼", state.icon)
    }

    @Test
    fun `Edit mode category khong con trong danh sach thi set categoryDeletedRemotely true`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(emptyList())
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_CATEGORY_ARG_CATEGORY_ID to "cat-404"))

        val viewModel = createViewModel(savedStateHandle)

        assertTrue(viewModel.uiState.value.categoryDeletedRemotely)
    }

    @Test
    fun `Edit mode da co name tu truoc (rotation) thi khong goi lai GetCategoriesUseCase`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                Routes.EDIT_CATEGORY_ARG_CATEGORY_ID to "cat-1",
                "name" to "Công việc" // key literal trùng CategoryFormViewModel.KEY_NAME (private)
            )
        )

        val viewModel = createViewModel(savedStateHandle)

        assertEquals("Công việc", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 0) { getCategoriesUseCase() }
    }

    @Test
    fun `Edit mode onSubmit thi goi UpdateCategoryUseCase voi dung id`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(existingCategory))
        coEvery { updateCategoryUseCase("cat-1", "Công việc gấp", "#1E88E5", "💼") } returns
            ApiResult.Success(existingCategory.copy(name = "Công việc gấp"))
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_CATEGORY_ARG_CATEGORY_ID to "cat-1"))
        val viewModel = createViewModel(savedStateHandle)

        viewModel.onNameChange("Công việc gấp")
        viewModel.onSubmit()

        coVerify(exactly = 1) { updateCategoryUseCase("cat-1", "Công việc gấp", "#1E88E5", "💼") }
        coVerify(exactly = 0) { createCategoryUseCase(any(), any(), any()) }
    }

    @Test
    fun `Edit mode onSubmit loi thi set submitError, khong set isSubmitSuccess`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(existingCategory))
        coEvery { updateCategoryUseCase("cat-1", "Công việc", "#1E88E5", "💼") } returns
            ApiResult.Error(ApiErrorCode.CONFLICT, "Tên danh mục đã tồn tại")
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_CATEGORY_ARG_CATEGORY_ID to "cat-1"))
        val viewModel = createViewModel(savedStateHandle)

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals("Tên danh mục đã tồn tại", state.submitError?.message)
        assertFalse(state.isSubmitSuccess)
    }

    @Test
    fun `Edit mode init loi NetworkError thi set submitError voi isNetworkError true`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.NetworkError
        val savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_CATEGORY_ARG_CATEGORY_ID to "cat-1"))

        val viewModel = createViewModel(savedStateHandle)

        assertTrue(viewModel.uiState.value.submitError?.isNetworkError == true)
    }
}
