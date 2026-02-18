package com.colma.colmaApp.features.presion.domain

import com.colma.colmaApp.utils.varios.HealthEntry

interface PresionRepository {
    suspend fun getHistory(): List<HealthEntry>
    suspend fun savePresion(
        sistolica: Double,
        diastolica: Double,
        dateTime: Long,
        advertencia: String?
    ): Result<HealthEntry>
}
