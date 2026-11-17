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
import org.junit.Test

/** Unit test cho [GetCategoriesUseCase] — pass-through, không có rule nghiệp vụ ở tầng này. */
class GetCategoriesUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk()
    private val useCase = GetCategoriesUseCase(categoryRepository)

    private val categories = listOf(
        Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼"),
        Category(id = "cat-2", name = "Mua sắm", color = "#43A047", icon = "🛒")
    )

    @Test
    fun `thanh cong thi tra dung danh sach tu Repository`() = runTest {
        coEvery { categoryRepository.getCategories() } returns ApiResult.Success(categories)

        val result = useCase()

        assertEquals(ApiResult.Success(categories), result)
        coVerify(exactly = 1) { categoryRepository.getCategories() }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error`() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            ApiResult.Error(ApiErrorCode.UNKNOWN, "Có lỗi xảy ra")

        val result = useCase()

        assertEquals(ApiResult.Error(ApiErrorCode.UNKNOWN, "Có lỗi xảy ra"), result)
    }

    @Test
    fun `Repository tra NetworkError thi tra nguyen NetworkError`() = runTest {
        coEvery { categoryRepository.getCategories() } returns ApiResult.NetworkError

        val result = useCase()

        assertEquals(ApiResult.NetworkError, result)
    }
}
