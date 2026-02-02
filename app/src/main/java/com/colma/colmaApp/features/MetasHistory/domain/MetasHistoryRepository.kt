package com.colma.colmaApp.features.MetasHistory.domain

import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos

interface MetasHistoryRepository {
    suspend fun getAllMetas(userId: Int): Result<List<RegistrosSaludModelos.MetaRecord>>
}
