package com.example.todo.domain.usecase.preferences

import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Unit test cho [SetInsertPositionPreferenceUseCase] — pass-through tới [PreferencesRepository]. */
class SetInsertPositionPreferenceUseCaseTest {

    private val preferencesRepository: PreferencesRepository = mockk()
    private val useCase = SetInsertPositionPreferenceUseCase(preferencesRepository)

    @Test
    fun `goi dung Repository voi TOP`() = runTest {
        coEvery { preferencesRepository.setInsertPosition(InsertPosition.TOP) } returns Unit

        useCase(InsertPosition.TOP)

        coVerify(exactly = 1) { preferencesRepository.setInsertPosition(InsertPosition.TOP) }
    }

    @Test
    fun `goi dung Repository voi BOTTOM`() = runTest {
        coEvery { preferencesRepository.setInsertPosition(InsertPosition.BOTTOM) } returns Unit

        useCase(InsertPosition.BOTTOM)

        coVerify(exactly = 1) { preferencesRepository.setInsertPosition(InsertPosition.BOTTOM) }
    }
}
