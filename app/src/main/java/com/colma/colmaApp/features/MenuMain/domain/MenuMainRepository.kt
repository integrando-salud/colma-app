package com.colma.colmaApp.features.MenuMain.domain

import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos

/**
 * Repository interface for the MenuMain feature.
 */
interface MenuMainRepository {
    suspend fun getPendingPastMeta(): RegistrosSaludModelos.MetaRecord?
    suspend fun getPendingFutureMetasCount(): Int
    suspend fun updateMeta(meta: RegistrosSaludModelos.MetaRecord)
    suspend fun synchronizeData(): Result<String>
    suspend fun saveSynchronizationRecord(timestamp: String, status: String, data: String?)
    suspend fun logout()
}
