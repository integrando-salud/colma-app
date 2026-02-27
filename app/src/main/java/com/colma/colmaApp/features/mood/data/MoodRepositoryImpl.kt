package com.colma.colmaApp.features.mood.data

import android.content.Context
import com.colma.colmaApp.features.mood.domain.MoodRepository
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.servicios.session.SessionManager
import com.colma.colmaApp.utils.varios.HealthEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodRepositoryImpl(private val context: Context) : MoodRepository {
    private val dbHelper = DBhelper(context)
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
            val all = dbHelper.getAllEstadoAnimo(null)
            all.mapNotNull { e ->
                val datetime = e.fechaEdicion ?: e.fechaRegistro
                val millis = parseDateTimeToMillis(datetime) ?: System.currentTimeMillis()
                val totalScore = e.preguntas.filterNotNull().sum()
                val display = "Puntuación: $totalScore"
                HealthEntry("Estado de ánimo", display, millis)
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun createDraftMoodRecord(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
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
            val idPaciente = 1 // hardcoded user id
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
}
