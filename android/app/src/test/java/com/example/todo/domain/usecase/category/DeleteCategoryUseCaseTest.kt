package com.example.todo.domain.usecase.category

import com.example.todo.domain.common.ApiErrorCode
import com.example.todo.domain.common.ApiResult
import com.example.todo.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit test cho [DeleteCategoryUseCase] — pass-through, không có rule nghiệp vụ ở tầng này. */
class DeleteCategoryUseCaseTest {

    private val categoryRepository: CategoryRepository = mockk()
    private val useCase = DeleteCategoryUseCase(categoryRepository)

    @Test
    fun `goi dung Repository voi id va tra Success Unit`() = runTest {
        coEvery { categoryRepository.deleteCategory("cat-1") } returns ApiResult.Success(Unit)

        val result = useCase("cat-1")

        assertEquals(ApiResult.Success(Unit), result)
        coVerify(exactly = 1) { categoryRepository.deleteCategory("cat-1") }
    }

    @Test
    fun `Repository tra Error thi tra nguyen Error`() = runTest {
        coEvery { categoryRepository.deleteCategory("cat-404") } returns
            ApiResult.Error(ApiErrorCode.NOT_FOUND, "Danh mục không tồn tại")

        val result = useCase("cat-404")

        assertEquals(ApiResult.Error(ApiErrorCode.NOT_FOUND, "Danh mục không tồn tại"), result)
    }
}
