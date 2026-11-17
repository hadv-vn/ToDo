package com.example.todo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.todo.domain.model.InsertPosition
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test cho [PreferencesRepositoryImpl] — FR-26 (docs/Task_Reorder_ToDo.md mục 5.2). Khác
 * [com.example.todo.data.local.SessionCookieStore] (phải test ở androidTest vì cần AndroidKeyStore
 * thật), [DataStore]<Preferences> ở đây KHÔNG mã hóa nên mock thẳng được bằng MockK, chạy JVM thuần.
 *
 * `updateData` là method THẬT của interface [DataStore] mà extension `edit{}` gọi xuống — mock nó
 * rồi tự áp dụng transform để lấy ra [Preferences] cuối cùng, xác nhận đúng key/value được ghi.
 */
class PreferencesRepositoryImplTest {

    private val dataStore: DataStore<Preferences> = mockk()
    private val repository = PreferencesRepositoryImpl(dataStore)

    private val insertPositionKey = stringPreferencesKey("insert_position")

    @Test
    fun `getInsertPosition tra BOTTOM khi DataStore dang luu gia tri bottom`() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf(insertPositionKey to "bottom"))

        val result = repository.getInsertPosition()

        assertEquals(InsertPosition.BOTTOM, result)
    }

    @Test
    fun `getInsertPosition tra TOP khi DataStore rong (mac dinh)`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        val result = repository.getInsertPosition()

        assertEquals(InsertPosition.TOP, result)
    }

    @Test
    fun `getInsertPosition tra TOP khi gia tri luu khac bottom`() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf(insertPositionKey to "top"))

        val result = repository.getInsertPosition()

        assertEquals(InsertPosition.TOP, result)
    }

    @Test
    fun `setInsertPosition BOTTOM thi ghi gia tri bottom vao dung key`() = runTest {
        var saved = emptyPreferences()
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            saved = transform(emptyPreferences())
            saved
        }

        repository.setInsertPosition(InsertPosition.BOTTOM)

        assertEquals("bottom", saved[insertPositionKey])
    }

    @Test
    fun `setInsertPosition TOP thi ghi gia tri top vao dung key`() = runTest {
        var saved = emptyPreferences()
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            saved = transform(preferencesOf(insertPositionKey to "bottom"))
            saved
        }

        repository.setInsertPosition(InsertPosition.TOP)

        assertEquals("top", saved[insertPositionKey])
    }
}
