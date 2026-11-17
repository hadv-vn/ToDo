package com.example.todo.presentation.category

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.repository.CategoryRepository
import com.example.todo.domain.usecase.category.CreateCategoryUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.domain.usecase.category.UpdateCategoryUseCase
import com.example.todo.presentation.navigation.Routes
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test cho luồng Sửa danh mục (FR-22, docs/Task_Category_ToDo.md mục 6.2).
 *
 * Cùng kỹ thuật [com.example.todo.presentation.taskform.EditTaskScreenTest]: dùng
 * `CompletableDeferred` để kiểm soát thời điểm [GetCategoriesUseCase] trả về, xác nhận đúng
 * trạng thái loading trước khi dữ liệu tới. Backend không có endpoint lấy 1 category theo id —
 * Edit mode tải qua GetCategoriesUseCase rồi tự tìm đúng categoryId (xem CategoryFormViewModel).
 */
class EditCategoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeCategoryRepository(
        private val pendingGetCategories: CompletableDeferred<ApiResult<List<Category>>>,
        private val pendingUpdateCategory: CompletableDeferred<ApiResult<Category>>? = null
    ) : CategoryRepository {
        var lastUpdateId: String? = null
        var lastUpdateName: String? = null
        var lastUpdateColor: String? = null
        var lastUpdateIcon: String? = null

        override suspend fun getCategories(): ApiResult<List<Category>> = pendingGetCategories.await()
        override suspend fun createCategory(name: String, color: String?, icon: String?) = throw NotImplementedError()
        override suspend fun updateCategory(id: String, name: String, color: String, icon: String): ApiResult<Category> {
            lastUpdateId = id
            lastUpdateName = name
            lastUpdateColor = color
            lastUpdateIcon = icon
            return pendingUpdateCategory?.await() ?: throw NotImplementedError("pendingUpdateCategory chưa được cấu hình")
        }
        override suspend fun deleteCategory(id: String) = throw NotImplementedError()
    }

    private fun createFormViewModel(
        repository: FakeCategoryRepository
    ): CategoryFormViewModel {
        return CategoryFormViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.EDIT_CATEGORY_ARG_CATEGORY_ID to CATEGORY_ID)),
            getCategoriesUseCase = GetCategoriesUseCase(repository),
            createCategoryUseCase = CreateCategoryUseCase(repository),
            updateCategoryUseCase = UpdateCategoryUseCase(repository)
        )
    }

    @Test
    fun dangTaiDuLieu_hienLoading_sauDoDienSanTen() {
        val pendingGet = CompletableDeferred<ApiResult<List<Category>>>()
        val repository = FakeCategoryRepository(pendingGet)

        composeTestRule.setContent {
            EditCategoryScreen(viewModel = createFormViewModel(repository))
        }

        composeTestRule.onNodeWithText("Tên danh mục").assertDoesNotExist()

        pendingGet.complete(ApiResult.Success(listOf(Category(id = CATEGORY_ID, name = "Công việc", color = "#1E88E5", icon = "💼"))))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Công việc").assertExists()
    }

    @Test
    fun danhMucDaBiXoaORemote_thiHienThongBaoVaTuDieuHuongVeList() {
        val pendingGet = CompletableDeferred<ApiResult<List<Category>>>()
        val repository = FakeCategoryRepository(pendingGet)
        var navigatedBack = false

        composeTestRule.setContent {
            EditCategoryScreen(viewModel = createFormViewModel(repository), onSubmitSuccess = { navigatedBack = true })
        }

        pendingGet.complete(ApiResult.Success(emptyList())) // category CATEGORY_ID không còn trong danh sách
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Danh mục này không còn tồn tại").assertExists()
        assertEquals(true, navigatedBack)
    }

    @Test
    fun suaTenHopLe_luuThanhCong_thiGoiUpdateCategoryUseCaseVoiDungId() {
        val pendingGet = CompletableDeferred<ApiResult<List<Category>>>()
        val pendingUpdate = CompletableDeferred<ApiResult<Category>>()
        val repository = FakeCategoryRepository(pendingGet, pendingUpdate)
        var navigatedBack = false

        composeTestRule.setContent {
            EditCategoryScreen(viewModel = createFormViewModel(repository), onSubmitSuccess = { navigatedBack = true })
        }

        pendingGet.complete(ApiResult.Success(listOf(Category(id = CATEGORY_ID, name = "Công việc", color = "#1E88E5", icon = "💼"))))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Công việc").performTextClearance()
        composeTestRule.onNodeWithText("Tên danh mục").performTextInput("Công việc gấp")
        composeTestRule.onNodeWithText("Lưu").performClick()

        pendingUpdate.complete(ApiResult.Success(Category(id = CATEGORY_ID, name = "Công việc gấp", color = "#1E88E5", icon = "💼")))
        composeTestRule.waitForIdle()

        assertEquals(CATEGORY_ID, repository.lastUpdateId)
        assertEquals("Công việc gấp", repository.lastUpdateName)
        assertEquals("#1E88E5", repository.lastUpdateColor)
        assertEquals("💼", repository.lastUpdateIcon)
        assertEquals(true, navigatedBack)
    }

    private companion object {
        const val CATEGORY_ID = "cat-1"
    }
}
