package com.colma.colmaApp.features.glucosa.data

import android.content.Context
import com.colma.colmaApp.features.glucosa.domain.GlucosaRepository
import com.colma.colmaApp.servicios.session.SessionManager
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.utils.varios.HealthEntry
import com.colma.colmaApp.utils.varios.parseDateTimeToMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlucosaRepositoryImpl(context: Context) : GlucosaRepository {

    private val dbHelper = DbHelper(context.applicationContext)
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
}
