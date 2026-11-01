package com.example.todo.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.todo.data.local.TokenLocalDataSource
import com.example.todo.data.local.TokenLocalDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val PREFERENCES_NAME = "todo_secure_prefs"

// DataStore<Preferences> property delegate — mỗi process chỉ có đúng 1 instance cho 1 file
// Preferences (yêu cầu bắt buộc của DataStore), nên khai báo ở top-level, KHÔNG tạo mới trong
// @Provides method (sẽ vi phạm "Multiple DataStores with the same name" nếu @Provides bị gọi lại).
private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

/**
 * Cung cấp DataStore<Preferences> singleton + [TokenLocalDataSource] — theo đề bài Phase 1 mục 6.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.secureDataStore
    }

    @Provides
    @Singleton
    fun provideTokenLocalDataSource(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>
    ): TokenLocalDataSource {
        return TokenLocalDataSourceImpl(context, dataStore)
    }
}
