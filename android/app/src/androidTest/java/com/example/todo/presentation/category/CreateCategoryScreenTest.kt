package com.example.todo.presentation.category

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.model.CategoryAppearance
import com.example.todo.domain.repository.CategoryRepository
import com.example.todo.domain.usecase.category.CreateCategoryUseCase
import com.example.todo.domain.usecase.category.GetCategoriesUseCase
import com.example.todo.domain.usecase.category.UpdateCategoryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test cho luồng Tạo danh mục (FR-22, docs/Task_Category_ToDo.md mục 6.2).
 *
 * Cùng pattern [com.example.todo.presentation.taskform.CreateTaskScreenTest]: nút "Lưu" tự
 * disable khi `name` rỗng/chỉ-khoảng-trắng (`enabled = !isSubmitting && name.isNotBlank()`).
 */
class CreateCategoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeCategoryRepository : CategoryRepository {
        var lastCreatedName: String? = null
        var lastCreatedColor: String? = null
        var lastCreatedIcon: String? = null

        override suspend fun getCategories(): ApiResult<List<Category>> = ApiResult.Success(emptyList())
        override suspend fun createCategory(name: String, color: String?, icon: String?): ApiResult<Category> {
            lastCreatedName = name
            lastCreatedColor = color
            lastCreatedIcon = icon
            return ApiResult.Success(Category(id = "cat-1", name = name, color = color ?: "", icon = icon ?: ""))
        }
        override suspend fun updateCategory(id: String, name: String, color: String, icon: String) =
            throw NotImplementedError()
        override suspend fun deleteCategory(id: String) = throw NotImplementedError()
    }

    private fun createFormViewModel(repository: FakeCategoryRepository = FakeCategoryRepository()): CategoryFormViewModel {
        return CategoryFormViewModel(
            savedStateHandle = SavedStateHandle(), // categoryId == null -> Create mode
            getCategoriesUseCase = GetCategoriesUseCase(repository),
            createCategoryUseCase = CreateCategoryUseCase(repository),
            updateCategoryUseCase = UpdateCategoryUseCase(repository)
        )
    }

    @Test
    fun tenRong_thiNutLuuBiDisable() {
        composeTestRule.setContent {
            CreateCategoryScreen(viewModel = createFormViewModel())
        }

        composeTestRule.onNodeWithText("Lưu").assertIsNotEnabled()
    }

    @Test
    fun nhapTenHopLe_thiNutLuuDuocBatLai() {
        composeTestRule.setContent {
            CreateCategoryScreen(viewModel = createFormViewModel())
        }

        composeTestRule.onNodeWithText("Tên danh mục").performTextInput("Công việc")

        composeTestRule.onNodeWithText("Lưu").assertIsEnabled()
    }

    @Test
    fun nhapTenRoiXoaHetVeRong_thiNutLuuTuDongDisableLai() {
        composeTestRule.setContent {
            CreateCategoryScreen(viewModel = createFormViewModel())
        }

        composeTestRule.onNodeWithText("Tên danh mục").performTextInput("A")
        composeTestRule.onNodeWithText("Lưu").assertIsEnabled()

        composeTestRule.onNodeWithText("A").performTextClearance()
        composeTestRule.onNodeWithText("Lưu").assertIsNotEnabled()
    }

    @Test
    fun khongChonIconKhac_thiTaoDanhMucVoiMauVaIconMacDinh() {
        val repository = FakeCategoryRepository()
        composeTestRule.setContent {
            CreateCategoryScreen(viewModel = createFormViewModel(repository))
        }

        composeTestRule.onNodeWithText("Tên danh mục").performTextInput("Công việc")
        composeTestRule.onNodeWithText("Lưu").performClick()

        assertEquals("Công việc", repository.lastCreatedName)
        assertEquals(CategoryAppearance.DEFAULT_COLOR, repository.lastCreatedColor)
        assertEquals(CategoryAppearance.DEFAULT_ICON, repository.lastCreatedIcon)
    }

    @Test
    fun chonIconKhac_thiTaoDanhMucVoiDungIconDaChon() {
        val repository = FakeCategoryRepository()
        composeTestRule.setContent {
            CreateCategoryScreen(viewModel = createFormViewModel(repository))
        }

        composeTestRule.onNodeWithText("Tên danh mục").performTextInput("Mua sắm")
        composeTestRule.onNodeWithText("🛒").performClick() // icon thứ 4 trong bộ cố định
        composeTestRule.onNodeWithText("Lưu").performClick()

        assertEquals("🛒", repository.lastCreatedIcon)
    }

    @Test
    fun luuThanhCong_thiGoiOnSubmitSuccess() {
        var submitSucceeded = false
        composeTestRule.setContent {
            CreateCategoryScreen(viewModel = createFormViewModel(), onSubmitSuccess = { submitSucceeded = true })
        }

        composeTestRule.onNodeWithText("Tên danh mục").performTextInput("Công việc")
        composeTestRule.onNodeWithText("Lưu").performClick()
        composeTestRule.waitForIdle()

        assertTrue(submitSucceeded)
    }
}
