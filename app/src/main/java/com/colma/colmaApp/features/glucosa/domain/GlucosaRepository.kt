package com.colma.colmaApp.features.glucosa.domain

import com.colma.colmaApp.utils.varios.HealthEntry

interface GlucosaRepository {
    suspend fun getHistory(): List<HealthEntry>
    suspend fun saveGlucosa(
        value: Double,
        isFasting: Boolean,
        dateTime: Long,
        advertencia: String?
    ): Result<HealthEntry>
    suspend fun getUserPaisId(): Int?
}
