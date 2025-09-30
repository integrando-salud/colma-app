package com.colma.colmaApp.servicios.database.dbhelper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DBhelper(context: Context) :

    SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val defaultDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val defaultTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    companion object {
        private const val DATABASE_NAME = "colmadb.db"
        private const val DATABASE_VERSION = 1
    }

    init {

        val db = writableDatabase
        if (!checkDatabaseExistence(db.path)) {
            try {
                copyDatabase(context)
            } catch (e: IOException) {
                throw RuntimeException("Error copying prepackaged database", e)
            }
        }

    }


    private fun checkDatabaseExistence(dbPath: String): Boolean {
        val file = File(dbPath)
        return file.exists()
    }


    @Throws(IOException::class)
    private fun copyDatabase(context: Context) {
        close()

        val outPath = getDatabasePath(context)
        File(outPath).parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }

        context.assets.open("databases/$DATABASE_NAME").use { input ->
            FileOutputStream(outPath).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
    }


    private fun getDatabasePath(context: Context): String = context.getDatabasePath(DATABASE_NAME).path

    fun insertGlucosa(
        valorMgdl: Double,
        idUsuario: Int? = null,
        fechaRegistro: String? = null,
        horaRegistro: String? = null
    ): Long {
        val db: SQLiteDatabase = writableDatabase
        val values = ContentValues().apply {
            put("valor_mgdl", valorMgdl)
            idUsuario?.let { put("idUsuario", it) }
            put("fechaRegistro", fechaRegistro ?: defaultDateFormat.format(Date()))
            put("horaRegistro", horaRegistro ?: defaultTimeFormat.format(Date()))
        }

        var rowId: Long = -1L
        db.beginTransaction()
        try {
            rowId = db.insert("glucosaRegistro", null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return rowId
    }

    fun insertPresion(
        sistolica: Double,
        diastolica: Double,
        idUsuario: Int? = null,
        fechaRegistro: String? = null,
        horaRegistro: String? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("sistolica", sistolica)
            put("diastolica", diastolica)
            idUsuario?.let { put("idUsuario", it) }
            put("fechaRegistro", fechaRegistro ?: defaultDateFormat.format(Date()))
            put("horaRegistro", horaRegistro ?: defaultTimeFormat.format(Date()))
        }

        var rowId: Long = -1L
        db.beginTransaction()
        try {
            rowId = db.insert("presionArterial", null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return rowId
    }

    fun getLatestGlucosa(idUsuario: Int? = null): RegistrosSaludModelos.GlucosaRecord? {
        val db = readableDatabase
        val selection = if (idUsuario != null) "idUsuario = ?" else null
        val selectionArgs = if (idUsuario != null) arrayOf(idUsuario.toString()) else null

        val cursor = db.query(
            "glucosaRegistro",
            arrayOf("idGlucosa", "idUsuario", "valor_mgdl", "fechaRegistro", "horaRegistro"),
            selection,
            selectionArgs,
            null,
            null,
            "idGlucosa DESC",
            "1"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                // Read using safe helpers
                val idLong = c.getLongOrNull("idGlucosa") ?: return null
                val idUsuarioVal = c.getIntOrNull("idUsuario")
                val valor = c.getDoubleOrNull("valor_mgdl")
                val fecha = c.getStringOrNull("fechaRegistro")
                val hora = c.getStringOrNull("horaRegistro")

                // If your RegistrosSaludModelos.GlucosaRecord expects Long for id use id = idLong
                // If it expects Int, convert: id = idLong.toInt()
                return RegistrosSaludModelos.GlucosaRecord(
                    id = idLong,               // <--- change .toInt() if your model uses Int
                    idUsuario = idUsuarioVal,
                    valorMgdl = valor,
                    fechaRegistro = fecha,
                    horaRegistro = hora
                )
            }
        }
        return null
    }

    fun getLatestPresion(idUsuario: Int? = null): RegistrosSaludModelos.PresionRecord? {
        val db = readableDatabase
        val selection = if (idUsuario != null) "idUsuario = ?" else null
        val selectionArgs = if (idUsuario != null) arrayOf(idUsuario.toString()) else null

        val cursor = db.query(
            "presionArterial",
            arrayOf("idPresion", "idUsuario", "sistolica", "diastolica", "fechaRegistro", "horaRegistro"),
            selection,
            selectionArgs,
            null,
            null,
            "idPresion DESC",
            "1"
        )

        cursor.use {
            if (it.moveToFirst()) {
                return RegistrosSaludModelos.PresionRecord(
                    id = it.getLong(it.getColumnIndexOrThrow("idPresion")),
                    idUsuario = it.getIntOrNull("idUsuario"),
                    sistolica = it.getDoubleOrNull("sistolica"),
                    diastolica = it.getDoubleOrNull("diastolica"),
                    fechaRegistro = it.getStringOrNull("fechaRegistro"),
                    horaRegistro = it.getStringOrNull("horaRegistro")
                )
            }
        }
        return null
    }
}

private fun Cursor.getLongOrNull(columnName: String): Long? {
    val idx = getColumnIndex(columnName)
    if (idx == -1 || isNull(idx)) return null
    return getLong(idx)
}

private fun Cursor.getIntOrNull(columnName: String): Int? {
    val idx = getColumnIndex(columnName)
    if (idx == -1 || isNull(idx)) return null
    return getInt(idx)
}

private fun Cursor.getDoubleOrNull(columnName: String): Double? {
    val idx = getColumnIndex(columnName)
    if (idx == -1 || isNull(idx)) return null
    return getDouble(idx)
}

private fun Cursor.getStringOrNull(columnName: String): String? {
    val idx = getColumnIndex(columnName)
    if (idx == -1 || isNull(idx)) return null
    return getString(idx)
}