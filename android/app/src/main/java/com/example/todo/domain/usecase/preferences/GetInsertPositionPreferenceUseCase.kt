package com.example.todo.domain.usecase.preferences

import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.repository.PreferencesRepository
import javax.inject.Inject

class GetInsertPositionPreferenceUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): InsertPosition = preferencesRepository.getInsertPosition()
}
