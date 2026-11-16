package com.example.todo.domain.repository

import com.example.todo.domain.model.InsertPosition

/**
 * Cài đặt lưu trên thiết bị, KHÔNG đồng bộ server/nhiều thiết bị — FR-26
 * (docs/Task_Reorder_ToDo.md mục 1/5.2). Tách interface riêng khỏi [TaskRepository] vì bản chất
 * khác hẳn (local-only, không qua HTTP) — SRP (CLAUDE.md nguyên tắc #3).
 */
interface PreferencesRepository {
    suspend fun getInsertPosition(): InsertPosition
    suspend fun setInsertPosition(position: InsertPosition)
}
