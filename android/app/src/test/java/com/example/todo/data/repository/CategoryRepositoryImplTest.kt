package com.example.todo.data.repository

import com.example.todo.data.remote.api.CategoryApiService
import com.example.todo.data.remote.dto.category.CategoryDto
import com.example.todo.data.remote.dto.category.CategoryEnvelopeDto
import com.example.todo.data.remote.dto.category.CategoryListEnvelopeDto
import com.example.todo.data.remote.dto.category.CreateCategoryRequestDto
import com.example.todo.data.remote.dto.category.UpdateCategoryRequestDto
import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Unit test cho [CategoryRepositoryImpl] — docs/Task_Category_ToDo.md mục 5.1, cùng pattern
 * [TaskRepositoryImplTest]: tập trung vào mapping DTO -> domain và truyền đúng request xuống
 * [CategoryApiService].
 */
class CategoryRepositoryImplTest {

    private val categoryApiService: CategoryApiService = mockk()
    private val repository = CategoryRepositoryImpl(categoryApiService)

    private val categoryDto = CategoryDto(
        id = "cat-1",
        name = "Công việc",
        color = "#1E88E5",
        icon = "💼",
        createdAt = "2026-10-14T08:30:00.000Z"
    )

    @Test
    fun `getCategories thanh cong thi map dung List CategoryDto sang List Category`() = runTest {
        coEvery { categoryApiService.getCategories() } returns
            Response.success(CategoryListEnvelopeDto(data = listOf(categoryDto)))

        val result = repository.getCategories()

        assertTrue(result is ApiResult.Success)
        val categories = (result as ApiResult.Success).data
        assertEquals(1, categories.size)
        assertEquals(Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼"), categories.first())
    }

    @Test
    fun `createCategory thi goi dung request va map ket qua`() = runTest {
        coEvery { categoryApiService.createCategory(CreateCategoryRequestDto(name = "Công việc", color = "#1E88E5", icon = "💼")) } returns
            Response.success(CategoryEnvelopeDto(data = categoryDto))

        val result = repository.createCategory("Công việc", "#1E88E5", "💼")

        assertTrue(result is ApiResult.Success)
        assertEquals("cat-1", (result as ApiResult.Success).data.id)
        coVerify(exactly = 1) {
            categoryApiService.createCategory(CreateCategoryRequestDto(name = "Công việc", color = "#1E88E5", icon = "💼"))
        }
    }

    @Test
    fun `createCategory voi color va icon null thi van gui request voi null (de backend tu chon mac dinh)`() = runTest {
        coEvery { categoryApiService.createCategory(CreateCategoryRequestDto(name = "Công việc", color = null, icon = null)) } returns
            Response.success(CategoryEnvelopeDto(data = categoryDto))

        repository.createCategory("Công việc", null, null)

        coVerify(exactly = 1) {
            categoryApiService.createCategory(CreateCategoryRequestDto(name = "Công việc", color = null, icon = null))
        }
    }

    @Test
    fun `updateCategory thi goi dung id va request voi du 3 field`() = runTest {
        val updatedDto = categoryDto.copy(name = "Mua sắm", color = "#43A047", icon = "🛒")
        coEvery { categoryApiService.updateCategory("cat-1", UpdateCategoryRequestDto(name = "Mua sắm", color = "#43A047", icon = "🛒")) } returns
            Response.success(CategoryEnvelopeDto(data = updatedDto))

        val result = repository.updateCategory("cat-1", "Mua sắm", "#43A047", "🛒")

        assertTrue(result is ApiResult.Success)
        assertEquals("Mua sắm", (result as ApiResult.Success).data.name)
        coVerify(exactly = 1) {
            categoryApiService.updateCategory("cat-1", UpdateCategoryRequestDto(name = "Mua sắm", color = "#43A047", icon = "🛒"))
        }
    }

    @Test
    fun `deleteCategory thanh cong 204 thi tra Success Unit`() = runTest {
        coEvery { categoryApiService.deleteCategory("cat-1") } returns Response.success<Void>(204, null)

        val result = repository.deleteCategory("cat-1")

        assertTrue(result is ApiResult.Success)
        assertEquals(Unit, (result as ApiResult.Success).data)
    }

    @Test
    fun `deleteCategory nhan 404 thi tra Error NOT_FOUND`() = runTest {
        val errorBody = "{\"error\":{\"code\":\"CATEGORY_NOT_FOUND\",\"message\":\"Danh mục không tồn tại\"}}"
            .toResponseBody("application/json".toMediaType())
        coEvery { categoryApiService.deleteCategory("cat-404") } returns Response.error(404, errorBody)

        val result = repository.deleteCategory("cat-404")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(ApiErrorCode.NOT_FOUND, error.code)
        assertEquals("Danh mục không tồn tại", error.message)
    }
}
