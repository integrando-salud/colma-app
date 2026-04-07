package com.colma.colmaApp.features.glucosa.data

import android.content.Context
import android.util.Log
import com.colma.colmaApp.features.glucosa.domain.GlucosaRepository
import com.colma.colmaApp.servicios.session.SessionManager
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.internetCommunication.SyncService
import com.colma.colmaApp.servicios.internetCommunication.model.GlucosaData
import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest
import com.colma.colmaApp.utils.NetworkUtils
import com.colma.colmaApp.utils.varios.HealthEntry
import com.colma.colmaApp.utils.varios.parseDateTimeToMillis
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlucosaRepositoryImpl(private val context: Context) : GlucosaRepository {

    private val TAG = "GlucosaRepo"
    private val dbHelper = DbHelper(context.applicationContext)
    private val syncService = SyncService()
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val userId: Int

    init {
        SessionManager.init(context.applicationContext)
        userId = SessionManager.getUserId() ?: 1 // Default to 1 if not found
    }

    override suspend fun getHistory(): List<HealthEntry> = withContext(Dispatchers.IO) {
        try {
            val all = dbHelper.getAllGlucosa(userId)
            val entries = all.mapNotNull { g ->
                val millis = parseDateTimeToMillis(g.fecha_hora_edicion) ?: System.currentTimeMillis()
                val valueText = g.valorMgdl?.let { v ->
                    if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.getDefault(), "%.1f", v)
                } ?: ""
                val fasting = (g.ayuno ?: 0) == 1
                val suffix = if (fasting) " · Ayuno" else " · Sin Ayuno"
                val display = "${valueText} mg/dl$suffix"
                HealthEntry(
                    id = g.id,
                    label = "Glucosa",
                    value = display,
                    timestamp = millis,
                    rawValue1 = g.valorMgdl,
                    isFasting = fasting
                )
            }
            entries.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun saveGlucosa(
        value: Double,
        isFasting: Boolean,
        dateTime: Long,
        advertencia: String?
    ): Result<HealthEntry> = withContext(Dispatchers.IO) {
        try {
            val fechaRegistroIso = isoFormatter.format(Date())
            val fechaEdicionIso = isoFormatter.format(Date(dateTime))

            val rowId = dbHelper.insertGlucosa(
                valorMgdl = value,
                idUsuario = userId,
                ayuno = isFasting,
                fechaRegistro = fechaRegistroIso,
                fechaHoraEdicion = fechaEdicionIso,
                advertencia = advertencia
            )

            if (rowId > 0L) {
                val valueText = if (value % 1.0 == 0.0)
                    value.toInt().toString()
                else
                    String.format(Locale.getDefault(), "%.1f", value)

                val suffix = if (isFasting) " · Ayuno" else " · Sin Ayuno"
                val display = "$valueText mg/dl$suffix"

                val newEntry = HealthEntry(
                    id = rowId,
                    label = "Glucosa",
                    value = display,
                    timestamp = dateTime,
                    rawValue1 = value,
                    isFasting = isFasting
                )
                Result.success(newEntry)
            } else {
                Result.failure(Exception("Error guardando en la base de datos"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getUserPaisId(): Int? = withContext(Dispatchers.IO) {
        try {
            dbHelper.getUsuarioPaisId(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun syncGlucosa(entry: HealthEntry, advertencia: String): Result<String> = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isInternetAvailable(context)) {
            Log.w(TAG, "Sync skipped: No internet connection.")
            return@withContext Result.failure(Exception("No hay conexión a internet"))
        }

        val token = SessionManager.getAuthToken() ?: run {
            Log.e(TAG, "Sync failed: No auth token found.")
            return@withContext Result.failure(Exception("No autenticado"))
        }
        val userId = SessionManager.getUserId() ?: run {
            Log.e(TAG, "Sync failed: No user ID found.")
            return@withContext Result.failure(Exception("Usuario no identificado"))
        }

        try {
            val now = isoFormatter.format(Date())
            val glucosaData = GlucosaData(
                idGlucosa = entry.id?.toInt() ?: 0,
                idUsuario = userId,
                valorMgdl = entry.rawValue1?.toFloat() ?: 0f,
                ayuno = if (entry.isFasting == true) 1 else 0,
                fechaRegistro = now, // Simplified
                fechaUsuario = isoFormatter.format(Date(entry.timestamp)),
                fechaSincronizacion = now,
                advertencia = advertencia 
            )

            val syncRequest = SyncDataRequest(
                deviceTimestamp = System.currentTimeMillis(),
                userId = userId,
                app = "colmaApp",
                clientSyncTimestamp = now,
                glucosa = listOf(glucosaData),
                presion = emptyList(),
                estadoAnimo = emptyList(),
                quiz = emptyList(),
                metas = emptyList(),
                multimediaRead = emptyList()
            )

            val requestBodyJson = Gson().toJson(syncRequest)
            Log.d(TAG, "Sync Request Body: $requestBodyJson")

            Log.d(TAG, "Attempting to sync glucose entry: ${entry.id}")
            val response = syncService.syncData(token, syncRequest)
            if (response.isSuccessful) {
                entry.id?.let { dbHelper.markGlucosaSent(listOf(it)) }
                Log.d(TAG, "Sync successful for entry ${entry.id}: ${response.body()?.message}")
                Result.success(response.body()?.message ?: "Sincronización exitosa")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Sync failed for entry ${entry.id}. Code: ${response.code()}, Error: $errorMsg")
                Result.failure(Exception("Error en sincronización: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sync for entry ${entry.id}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
