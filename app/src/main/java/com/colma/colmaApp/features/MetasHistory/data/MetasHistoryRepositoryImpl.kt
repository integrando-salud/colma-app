package com.colma.colmaApp.features.MetasHistory.data

import android.content.Context
import com.colma.colmaApp.features.MetasHistory.domain.MetasHistoryRepository
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetasHistoryRepositoryImpl(private val context: Context) : MetasHistoryRepository {

    override suspend fun getAllMetas(userId: Int): Result<List<RegistrosSaludModelos.MetaRecord>> {
        return withContext(Dispatchers.IO) {
            try {
                val dbHelper = DbHelper(context)
                val metas = dbHelper.getAllMetas(userId)
                Result.success(metas)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
