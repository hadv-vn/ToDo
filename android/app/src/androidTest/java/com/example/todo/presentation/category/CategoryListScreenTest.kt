package com.example.todo.presentation.category

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.repository.CategoryRepository
import com.example.todo.domain.usecase.category.DeleteCategoryUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test cho màn hình Quản lý danh mục (FR-22, docs/Task_Category_ToDo.md mục 6.1).
 *
 * Không dùng MockK (androidTestImplementation chưa có `mockk-android`) — dùng [FakeCategoryRepository]
 * viết tay implement [CategoryRepository], cùng pattern các test Compose khác trong project.
 */
class CategoryListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categoryWork = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")
    private val categoryShopping = Category(id = "cat-2", name = "Mua sắm", color = "#43A047", icon = "🛒")

    private class FakeCategoryRepository(
        private val categories: List<Category>,
        private val deleteResult: ApiResult<Unit> = ApiResult.Success(Unit)
    ) : CategoryRepository {
        override suspend fun getCategories(): ApiResult<List<Category>> = ApiResult.Success(categories)
        override suspend fun createCategory(name: String, color: String?, icon: String?) = throw NotImplementedError()
        override suspend fun updateCategory(id: String, name: String, color: String, icon: String) =
            throw NotImplementedError()
        override suspend fun deleteCategory(id: String): ApiResult<Unit> = deleteResult
    }

    private fun createListViewModel(categories: List<Category>): CategoryListViewModel {
        val repository = FakeCategoryRepository(categories)
        return CategoryListViewModel(
            getCategoriesUseCase = GetCategoriesUseCase(repository),
            deleteCategoryUseCase = DeleteCategoryUseCase(repository)
        )
    }

    @Test
    fun coDanhSachDanhMuc_thiHienDungTenVaIcon() {
        composeTestRule.setContent {
            CategoryListScreen(viewModel = createListViewModel(listOf(categoryWork, categoryShopping)))
        }

        composeTestRule.onNodeWithText("💼 Công việc").assertExists()
        composeTestRule.onNodeWithText("🛒 Mua sắm").assertExists()
    }

    @Test
    fun chuaCoDanhMucNao_thiHienEmptyState() {
        composeTestRule.setContent {
            CategoryListScreen(viewModel = createListViewModel(emptyList()))
        }

        composeTestRule.onNodeWithText("Bạn chưa có danh mục nào. Nhấn + để tạo danh mục đầu tiên.").assertExists()
    }

    @Test
    fun bamVaoRow_thiGoiCallbackVoiDungCategoryId() {
        var editedCategoryId: String? = null
        composeTestRule.setContent {
            CategoryListScreen(
                viewModel = createListViewModel(listOf(categoryWork)),
                onNavigateToEditCategory = { editedCategoryId = it }
            )
        }

        composeTestRule.onNodeWithText("💼 Công việc").performClick()

        assertEquals("cat-1", editedCategoryId)
    }

    @Test
    fun bamXoaRoiXacNhan_thiLoaiDanhMucKhoiDanhSach() {
        composeTestRule.setContent {
            CategoryListScreen(viewModel = createListViewModel(listOf(categoryWork)))
        }

        composeTestRule.onNodeWithText("Xóa").performClick() // mở dialog xác nhận
        composeTestRule.onNodeWithText("Xóa danh mục").assertExists()

        composeTestRule.onAllNodesWithText("Xóa").onLast().performClick() // nút xác nhận trong dialog

        composeTestRule.onAllNodesWithText("💼 Công việc").assertCountEquals(0)
    }

    @Test
    fun bamXoaRoiHuy_thiDanhMucVanConTrongDanhSach() {
        composeTestRule.setContent {
            CategoryListScreen(viewModel = createListViewModel(listOf(categoryWork)))
        }

        composeTestRule.onNodeWithText("Xóa").performClick()
        composeTestRule.onNodeWithText("Hủy").performClick()

        composeTestRule.onAllNodesWithText("Xóa danh mục").assertCountEquals(0)
        composeTestRule.onNodeWithText("💼 Công việc").assertExists()
    }

    @Test
    fun bamNutThem_thiGoiCallbackOnNavigateToCreateCategory() {
        var navigated = false
        composeTestRule.setContent {
            CategoryListScreen(
                viewModel = createListViewModel(emptyList()),
                onNavigateToCreateCategory = { navigated = true }
            )
        }

        composeTestRule.onNodeWithText("+").performClick()

        assertEquals(true, navigated)
    }
}
