package com.colma.colmaApp.features.presion.data

import android.content.Context
import android.util.Log
import com.colma.colmaApp.servicios.session.SessionManager
import com.colma.colmaApp.features.presion.domain.PresionRepository
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.internetCommunication.SyncService
import com.colma.colmaApp.servicios.internetCommunication.model.PresionData
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

class PresionRepositoryImpl(private val context: Context) : PresionRepository {

    private val TAG = "PresionRepo"
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
            val all = dbHelper.getAllPresion(userId)

            fun fmt(v: Double?) = when {
                v == null -> ""
                v % 1.0 == 0.0 -> v.toInt().toString()
                else -> String.format(Locale.getDefault(), "%.1f", v)
            }

            val entries = all.mapNotNull { p ->
                val millis = parseDateTimeToMillis(p.fecha_hora_edicion) ?: System.currentTimeMillis()
                val syst = fmt(p.sistolica)
                val dia = fmt(p.diastolica)
                val valueText = if (syst.isNotBlank() && dia.isNotBlank()) "$syst/$dia mmHg" else ""
                HealthEntry(
                    id = p.id,
                    label = "Presion",
                    value = valueText,
                    timestamp = millis,
                    rawValue1 = p.sistolica,
                    rawValue2 = p.diastolica
                )
            }.sortedByDescending { it.timestamp }
            entries
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun savePresion(
        sistolica: Double,
        diastolica: Double,
        dateTime: Long,
        advertencia: String?
    ): Result<HealthEntry> = withContext(Dispatchers.IO) {
        try {
            val fechaRegistroIso = isoFormatter.format(Date())
            val fechaEdicionIso = isoFormatter.format(Date(dateTime))
            val rowId = dbHelper.insertPresion(
                sistolica = sistolica,
                diastolica = diastolica,
                idUsuario = userId,
                fechaRegistro = fechaRegistroIso,
                fechaHoraEdicionIso = fechaEdicionIso,
                advertencia = advertencia
            )

            if (rowId > 0L) {
                val lastPresionValue = if (sistolica % 1.0 == 0.0 && diastolica % 1.0 == 0.0) {
                    "${sistolica.toInt()}/${diastolica.toInt()}"
                } else {
                    "${String.format(Locale.getDefault(), "%.1f", sistolica)}/${String.format(Locale.getDefault(), "%.1f", diastolica)}"
                }

                val newEntry = HealthEntry(
                    id = rowId,
                    label = "Presion",
                    value = "$lastPresionValue mmHg",
                    timestamp = dateTime,
                    rawValue1 = sistolica,
                    rawValue2 = diastolica
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

    override suspend fun syncPresion(entry: HealthEntry, advertencia: String): Result<String> = withContext(Dispatchers.IO) {
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
            val presionData = PresionData(
                idPresion = entry.id?.toInt() ?: 0,
                idUsuario = userId,
                sistolica = entry.rawValue1?.toFloat() ?: 0f,
                diastolica = entry.rawValue2?.toFloat() ?: 0f,
                fechaRegistro = now,
                fechaUsuario = isoFormatter.format(Date(entry.timestamp)),
                fechaSincronizacion = now,
                advertencia = advertencia
            )

            val syncRequest = SyncDataRequest(
                deviceTimestamp = System.currentTimeMillis(),
                userId = userId,
                app = "colmaApp",
                clientSyncTimestamp = now,
                glucosa = emptyList(),
                presion = listOf(presionData),
                estadoAnimo = emptyList(),
                quiz = emptyList(),
                metas = emptyList(),
                multimediaRead = emptyList()
            )

            val requestBodyJson = Gson().toJson(syncRequest)
            Log.d(TAG, "Sync Request Body: $requestBodyJson")

            Log.d(TAG, "Attempting to sync pressure entry: ${entry.id}")
            val response = syncService.syncData(token, syncRequest)
            if (response.isSuccessful) {
                entry.id?.let { dbHelper.markPresionSent(listOf(it)) }
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

    override suspend fun checkHighPressureTrend(): Boolean = withContext(Dispatchers.IO) {
        dbHelper.hasHighPressureTrend(userId)
    }
}
