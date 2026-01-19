package com.colma.colmaApp.features.presion.data

import android.content.Context
import com.colma.colmaApp.features.presion.domain.PresionRepository
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper
import com.colma.colmaApp.utils.varios.HealthEntry
import com.colma.colmaApp.utils.varios.parseDateTimeToMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PresionRepositoryImpl(context: Context) : PresionRepository {

    private val dbHelper = DBhelper(context.applicationContext)
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override suspend fun getHistory(): List<HealthEntry> = withContext(Dispatchers.IO) {
        try {
            val all = dbHelper.getAllPresion(null)

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
        dateTime: Long
    ): Result<HealthEntry> = withContext(Dispatchers.IO) {
        try {
            val fechaRegistroIso = isoFormatter.format(Date())
            val fechaEdicionIso = isoFormatter.format(Date(dateTime))
            val rowId = dbHelper.insertPresion(
                sistolica = sistolica,
                diastolica = diastolica,
                idUsuario = 1,
                fechaRegistro = fechaRegistroIso,
                fechaHoraEdicionIso = fechaEdicionIso
            )

            if (rowId > 0L) {
                val lastPresionValue = if (sistolica % 1.0 == 0.0 && diastolica % 1.0 == 0.0) {
                    "${sistolica.toInt()}/${diastolica.toInt()}"
                } else {
                    "${String.format(Locale.getDefault(), "%.1f", sistolica)}/${String.format(Locale.getDefault(), "%.1f", diastolica)}"
                }

                val newEntry = HealthEntry(
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
}
