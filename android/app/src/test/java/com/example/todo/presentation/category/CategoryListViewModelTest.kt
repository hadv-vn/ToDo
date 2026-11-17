package com.example.todo.presentation.category

import app.cash.turbine.test
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.usecase.category.DeleteCategoryUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
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

/** Unit test cho [CategoryListViewModel] — FR-22 (docs/Task_Category_ToDo.md mục 5.3), cùng khuôn [com.example.todo.presentation.tasklist.TaskListViewModelTest]. */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getCategoriesUseCase: GetCategoriesUseCase = mockk()
    private val deleteCategoryUseCase: DeleteCategoryUseCase = mockk()

    private val categoryWork = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")
    private val categoryShopping = Category(id = "cat-2", name = "Mua sắm", color = "#43A047", icon = "🛒")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CategoryListViewModel =
        CategoryListViewModel(getCategoriesUseCase, deleteCategoryUseCase)

    @Test
    fun `init thanh cong thi nap danh sach categories va tat isLoading`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(categoryWork, categoryShopping))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(categoryWork, categoryShopping), state.categories)
    }

    @Test
    fun `init loi Error thi set error va isLoading false`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Error(ApiErrorCode.SERVER_ERROR, "Có lỗi xảy ra")

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Có lỗi xảy ra", state.error?.message)
    }

    @Test
    fun `init loi NetworkError thi set error voi isNetworkError true`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.NetworkError

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.error?.isNetworkError == true)
    }

    @Test
    fun `onDeleteCategoryConfirmed thanh cong thi loai category khoi danh sach`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(categoryWork, categoryShopping))
        coEvery { deleteCategoryUseCase("cat-1") } returns ApiResult.Success(Unit)

        val viewModel = createViewModel()
        viewModel.onDeleteCategoryConfirmed("cat-1")

        val state = viewModel.uiState.value
        assertEquals(listOf(categoryShopping), state.categories)
        assertFalse("cat-1" in state.pendingDeleteCategoryIds)
    }

    @Test
    fun `onDeleteCategoryConfirmed goi 2 lan lien tiep cho cung id thi chi goi UseCase 1 lan (chan double-confirm)`() =
        runTest(testDispatcher) {
            coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(categoryWork))
            val pending = CompletableDeferred<ApiResult<Unit>>()
            coEvery { deleteCategoryUseCase("cat-1") } coAnswers { pending.await() }

            val viewModel = createViewModel()
            viewModel.onDeleteCategoryConfirmed("cat-1")
            viewModel.onDeleteCategoryConfirmed("cat-1")

            coVerify(exactly = 1) { deleteCategoryUseCase("cat-1") }
            pending.complete(ApiResult.Success(Unit))
        }

    @Test
    fun `onDeleteCategoryConfirmed loi thi giu category, bo pendingDelete va bao loi`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(categoryWork))
        coEvery { deleteCategoryUseCase("cat-1") } returns
            ApiResult.Error(ApiErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // sau init load thành công

            viewModel.onDeleteCategoryConfirmed("cat-1")
            val pending = awaitItem()
            assertTrue("cat-1" in pending.pendingDeleteCategoryIds)

            val afterError = awaitItem()
            assertEquals(listOf(categoryWork), afterError.categories) // vẫn còn nguyên, KHÔNG xóa
            assertFalse("cat-1" in afterError.pendingDeleteCategoryIds)
            assertEquals("Bạn không có quyền thực hiện thao tác này", afterError.error?.message)
        }
    }

    @Test
    fun `onErrorConsumed xoa error khoi state`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Error(ApiErrorCode.SERVER_ERROR, "Có lỗi xảy ra")

        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.error != null)

        viewModel.onErrorConsumed()

        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `loadCategories goi lai thi nap danh sach moi`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(categoryWork))
        val viewModel = createViewModel()
        assertEquals(listOf(categoryWork), viewModel.uiState.value.categories)

        coEvery { getCategoriesUseCase() } returns ApiResult.Success(listOf(categoryWork, categoryShopping))
        viewModel.loadCategories()

        assertEquals(listOf(categoryWork, categoryShopping), viewModel.uiState.value.categories)
    }
}
