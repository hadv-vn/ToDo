package com.example.todo.domain.usecase.preferences

import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit test cho [GetInsertPositionPreferenceUseCase] — pass-through tới [PreferencesRepository]. */
class GetInsertPositionPreferenceUseCaseTest {

    private val preferencesRepository: PreferencesRepository = mockk()
    private val useCase = GetInsertPositionPreferenceUseCase(preferencesRepository)

    @Test
    fun `tra dung gia tri TOP tu Repository`() = runTest {
        coEvery { preferencesRepository.getInsertPosition() } returns InsertPosition.TOP

        val result = useCase()

        assertEquals(InsertPosition.TOP, result)
        coVerify(exactly = 1) { preferencesRepository.getInsertPosition() }
    }

    @Test
    fun `tra dung gia tri BOTTOM tu Repository`() = runTest {
        coEvery { preferencesRepository.getInsertPosition() } returns InsertPosition.BOTTOM

        val result = useCase()

        assertEquals(InsertPosition.BOTTOM, result)
        coVerify(exactly = 1) { preferencesRepository.getInsertPosition() }
    }
}
