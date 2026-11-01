package com.example.todo.di

import com.example.todo.data.repository.AuthRepositoryImpl
import com.example.todo.data.repository.TaskRepositoryImpl
import com.example.todo.domain.repository.AuthRepository
import com.example.todo.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds interface Domain -> implementation thật Data — theo đề bài Phase 1 mục 6.
 * Phase 2: thêm TaskRepository -> TaskRepositoryImpl.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
