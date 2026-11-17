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

/**
 * Unit test cho [CreateCategoryUseCase] — docs/Task_Category_ToDo.md mục 5.1: validate
 * name.trim().isNotEmpty() mang tính defensive TRƯỚC khi gọi Repository, cùng tinh thần
 * [com.example.todo.domain.usecase.task.CreateTaskUseCase].
 */
class CreateCategoryUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk()
    private val useCase = CreateCategoryUseCase(categoryRepository)

    private val createdCategory = Category(id = "cat-1", name = "Công việc", color = "#1E88E5", icon = "💼")

    @Test
    fun `name rong thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("", "#1E88E5", "💼")

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { categoryRepository.createCategory(any(), any(), any()) }
    }

    @Test
    fun `name chi co khoang trang thi tra Error BAD_REQUEST va khong goi Repository`() = runTest {
        val result = useCase("   ", null, null)

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorCode.BAD_REQUEST, (result as ApiResult.Error).code)
        coVerify(exactly = 0) { categoryRepository.createCategory(any(), any(), any()) }
    }

    @Test
    fun `name hop le thi goi Repository voi name da trim`() = runTest {
        coEvery { categoryRepository.createCategory("Công việc", "#1E88E5", "💼") } returns
            ApiResult.Success(createdCategory)

        val result = useCase("  Công việc  ", "#1E88E5", "💼")

        assertEquals(ApiResult.Success(createdCategory), result)
        coVerify(exactly = 1) { categoryRepository.createCategory("Công việc", "#1E88E5", "💼") }
    }

    @Test
    fun `color va icon null thi van goi Repository voi null (dung mac dinh backend)`() = runTest {
        coEvery { categoryRepository.createCategory("Công việc", null, null) } returns
            ApiResult.Success(createdCategory)

        val result = useCase("Công việc", null, null)

        assertEquals(ApiResult.Success(createdCategory), result)
        coVerify(exactly = 1) { categoryRepository.createCategory("Công việc", null, null) }
    }
}
