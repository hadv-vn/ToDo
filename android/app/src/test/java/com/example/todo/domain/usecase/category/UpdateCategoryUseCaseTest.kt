package com.example.todo.domain.usecase.category

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.model.Category
import com.example.todo.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit test cho [UpdateCategoryUseCase] — cùng rule validate name với [CreateCategoryUseCaseTest]. */
class UpdateCategoryUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk()
    private val useCase = UpdateCategoryUseCase(categoryRepository)

    private val updatedCategory = Category(id = "cat-1", name = "Mua sắm", color = "#43A047", icon = "🛒")

    @Test
    fun `name rong thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("cat-1", "", "#43A047", "🛒")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { categoryRepository.updateCategory(any(), any(), any(), any()) }
    }

    @Test
    fun `name chi co khoang trang thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("cat-1", "\t\n ", "#43A047", "🛒")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { categoryRepository.updateCategory(any(), any(), any(), any()) }
    }

    @Test
    fun `name hop le thi goi Repository updateCategory voi dung id va name da trim`() = runTest {
        coEvery { categoryRepository.updateCategory("cat-1", "Mua sắm", "#43A047", "🛒") } returns
            ApiResult.Success(updatedCategory)

        val result = useCase("cat-1", "  Mua sắm  ", "#43A047", "🛒")

        assertEquals(ApiResult.Success(updatedCategory), result)
        coVerify(exactly = 1) { categoryRepository.updateCategory("cat-1", "Mua sắm", "#43A047", "🛒") }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error`() = runTest {
        coEvery { categoryRepository.updateCategory("cat-404", "Mua sắm", "#43A047", "🛒") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Danh mục không tồn tại")

        val result = useCase("cat-404", "Mua sắm", "#43A047", "🛒")

        assertEquals(ApiResult.Error(ApiErrorCode.NOT_FOUND, "Danh mục không tồn tại"), result)
    }
}
