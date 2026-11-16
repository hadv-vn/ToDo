package com.example.todo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.todo.domain.model.InsertPosition
import com.example.todo.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * FR-26 (docs/Task_Reorder_ToDo.md mục 5.2) — tái dùng CHÍNH [DataStore] đang dùng cho
 * [com.example.todo.data.local.SessionCookieStore] (cùng instance qua DI, xem DataStoreModule),
 * KHÔNG tạo thêm hệ thống lưu trữ khác (nguyên tắc #9). Giá trị này không nhạy cảm (chỉ là 1 UI
 * preference) nên không cần mã hóa như cookie session.
 */
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    override suspend fun getInsertPosition(): InsertPosition {
        val stored = dataStore.data.first()[KEY_INSERT_POSITION]
        return if (stored == "bottom") InsertPosition.BOTTOM else InsertPosition.TOP
    }

    override suspend fun setInsertPosition(position: InsertPosition) {
        dataStore.edit { prefs ->
            prefs[KEY_INSERT_POSITION] = if (position == InsertPosition.BOTTOM) "bottom" else "top"
        }
    }

    private companion object {
        val KEY_INSERT_POSITION = stringPreferencesKey("insert_position")
    }
}
