package com.colma.colmaApp.features.mood.data

import android.content.Context
import android.util.Log
import com.colma.colmaApp.features.mood.domain.MoodRepository
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.internetCommunication.SyncService
import com.colma.colmaApp.servicios.internetCommunication.model.EstadoAnimoData
import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest
import com.colma.colmaApp.servicios.session.SessionManager
import com.colma.colmaApp.utils.NetworkUtils
import com.colma.colmaApp.utils.varios.HealthEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodRepositoryImpl(private val context: Context) : MoodRepository {
    private val TAG = "MoodRepo"
    private val dbHelper = DbHelper(context)
    private val syncService = SyncService()
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val userId: Int

    init {
        SessionManager.init(context.applicationContext)
        userId = SessionManager.getUserId() ?: 1 // Default to 1 if not found
    }

    override suspend fun getMoodHistory(): List<HealthEntry> = withContext(Dispatchers.IO) {
        fun parseDateTimeToMillis(datetime: String?): Long? {
            if (datetime.isNullOrBlank()) return null
            val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return try {
                iso.parse(datetime)?.time
            } catch (e: ParseException) {
                try {
                    datetime.toLong()
                } catch (ex: Exception) {
                    null
                }
            }
        }

        try {
            val all = dbHelper.getAllEstadoAnimo(userId)
            all.mapNotNull { e ->
                val datetime = e.fechaEdicion ?: e.fechaRegistro
                val millis = parseDateTimeToMillis(datetime) ?: System.currentTimeMillis()
                val totalScore = e.preguntas.filterNotNull().sum()
                val display = "Puntuación: $totalScore"
                HealthEntry(id = e.rowid, label = "Estado de ánimo", value = display, timestamp = millis)
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getLastMoodDate(): String? = withContext(Dispatchers.IO) {
        try {
            dbHelper.getLatestEstadoAnimoDate(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun createDraftMoodRecord(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val idPaciente = userId
            val fechaEdicionIso = isoFormatter.format(Date(System.currentTimeMillis()))
            val newId = dbHelper.insertEstadoAnimo(idPaciente, emptyMap(), fechaRegistro = null, fechaEdicion = fechaEdicionIso)
            if (newId > 0L) {
                Result.success(newId)
            } else {
                Result.failure(Exception("Failed to create draft mood record"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMoodRecord(recordId: Long, answers: Map<Int, Int?>, editDate: String) = withContext(Dispatchers.IO) {
        dbHelper.updateEstadoAnimoRow(recordId, answers, fechaEdicion = editDate)
    }

    override suspend fun saveMoodRecord(answers: Map<Int, Int?>, editDate: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val idPaciente = userId
            val newId = dbHelper.insertEstadoAnimo(idPaciente, answers, fechaRegistro = null, fechaEdicion = editDate)
            if (newId > 0L) {
                Result.success(newId)
            } else {
                Result.failure(Exception("Failed to save mood record"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMoodRecord(recordId: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteEstadoAnimoRow(recordId)
    }

    override suspend fun syncMoodRecord(recordId: Long): Result<String> = withContext(Dispatchers.IO) {
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

        val record = dbHelper.getEstadoAnimoByRowId(recordId) ?: run {
            Log.e(TAG, "Sync failed: Record not found in DB for ID $recordId.")
            return@withContext Result.failure(Exception("Registro no encontrado"))
        }

        try {
            val now = isoFormatter.format(Date())
            val estadoAnimoData = EstadoAnimoData(
                idEstadoAnimo = record.rowid.toInt(),
                idUsuario = userId,
                fechaRegistro = record.fechaRegistro ?: now,
                fechaUsuario = record.fechaEdicion ?: record.fechaRegistro ?: now,
                pregunta1 = record.preguntas.getOrNull(0) ?: 0,
                pregunta2 = record.preguntas.getOrNull(1) ?: 0,
                pregunta3 = record.preguntas.getOrNull(2) ?: 0,
                pregunta4 = record.preguntas.getOrNull(3) ?: 0,
                pregunta5 = record.preguntas.getOrNull(4) ?: 0,
                pregunta6 = record.preguntas.getOrNull(5) ?: 0,
                pregunta7 = record.preguntas.getOrNull(6) ?: 0,
                pregunta8 = record.preguntas.getOrNull(7) ?: 0,
                pregunta9 = record.preguntas.getOrNull(8) ?: 0,
                fechaSincronizacion = now
            )

            val syncRequest = SyncDataRequest(
                deviceTimestamp = System.currentTimeMillis(),
                userId = userId,
                app = "colmaApp",
                clientSyncTimestamp = now,
                glucosa = emptyList(),
                presion = emptyList(),
                estadoAnimo = listOf(estadoAnimoData),
                quiz = emptyList(),
                metas = emptyList(),
                multimediaRead = emptyList()
            )

            Log.d(TAG, "Attempting to sync mood record: $recordId")
            val response = syncService.syncData(token, syncRequest)
            if (response.isSuccessful) {
                dbHelper.updateEstadoAnimoSyncTimestamps(mapOf(recordId to now))
                Log.d(TAG, "Sync successful for mood record $recordId: ${response.body()?.message}")
                Result.success(response.body()?.message ?: "Sincronización exitosa")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Sync failed for mood record $recordId. Code: ${response.code()}, Error: $errorMsg")
                Result.failure(Exception("Error en sincronización: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sync for mood record $recordId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
