package com.colma.colmaApp.features.mood.domain

import com.colma.colmaApp.utils.varios.HealthEntry

interface MoodRepository {
    suspend fun getMoodHistory(): List<HealthEntry>
    suspend fun createDraftMoodRecord(): Result<Long>
    suspend fun updateMoodRecord(recordId: Long, answers: Map<Int, Int?>, editDate: String)
    suspend fun deleteMoodRecord(recordId: Long)
}
