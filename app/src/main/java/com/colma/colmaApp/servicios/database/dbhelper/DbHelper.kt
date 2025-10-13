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

    private val defaultDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

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

    // -------------------------
    // Inserts
    // -------------------------

    /**
     * Insert into glucosa_table
     * - valorMgdl required
     * - idUsuario optional
     * - ayuno optional (0/1) - use null if unknown
     * - fechaRegistro optional (defaults to today as yyyy-MM-dd)
     * - fecha_sincronizacion defaults to NULL (unsynced)
     */
    fun insertGlucosa(
        valorMgdl: Double,
        idUsuario: Int? = null,
        ayuno: Boolean? = null,                 // NEW
        fechaRegistro: String? = null
    ): Long {
        val db: SQLiteDatabase = writableDatabase
        val values = ContentValues().apply {
            put("valor_mgdl", valorMgdl)
            idUsuario?.let { put("id_usuario", it) }
            // save full datetime in "yyyy-MM-dd HH:mm:ss"
            put("fecha_registro", fechaRegistro ?: defaultDateTimeFormat.format(Date()))
            // only put 'ayuno' if provided
            ayuno?.let { put("ayuno", if (it) 1 else 0) }
        }

        var rowId: Long = -1L
        db.beginTransaction()
        try {
            rowId = db.insert("glucosa_table", null, values) // using new table name
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return rowId
    }

    /**
     * Insert into presion_table
     * - sistolica / diastolica required
     * - idUsuario optional
     * - fecha_registro optional (defaults to today)
     * - fecha_sincronizacion optional
     */
    fun insertPresion(
        sistolica: Double,
        diastolica: Double,
        idUsuario: Int? = null,
        fechaRegistro: String? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("sistolica", sistolica)
            put("diastolica", diastolica)
            idUsuario?.let { put("id_usuario", it) }
            put("fecha_registro", fechaRegistro ?: defaultDateTimeFormat.format(Date()))
            // fecha_sincronizacion left null until synced
        }

        var rowId: Long = -1L
        db.beginTransaction()
        try {
            rowId = db.insert("presion_table", null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return rowId
    }

    // -------------------------
    // Latest (single) getters
    // -------------------------

    /**
     * Latest glucosa (newest id_glucosa)
     */
    fun getLatestGlucosa(idUsuario: Int? = null): RegistrosSaludModelos.GlucosaRecord? {
        val db = readableDatabase
        val selection = idUsuario?.let { "id_usuario = ?" }
        val selectionArgs = idUsuario?.let { arrayOf(it.toString()) }

        val cursor = db.query(
            "glucosa_table",
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "fecha_registro", "ayuno"),
            selection,
            selectionArgs,
            null,
            null,
            "id_glucosa DESC",
            "1"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                val idLong = c.getLongOrNull("id_glucosa") ?: return null
                val idUsuarioVal = c.getIntOrNull("id_usuario")
                val valor = c.getDoubleOrNull("valor_mgdl")
                val fecha = c.getStringOrNull("fecha_registro")
                // read ayuno as Int? (0/1) to match your model type
                val ayunoInt: Int? = c.getIntOrNull("ayuno")

                return RegistrosSaludModelos.GlucosaRecord(
                    id = idLong,
                    idUsuario = idUsuarioVal,
                    valorMgdl = valor,
                    fecha_registro = fecha,
                    ayuno = ayunoInt,              // pass Int? (0/1/null)
                    fecha_sincronizacion = null
                )
            }
        }
        return null
    }

    /**
     * Latest presion (newest id_presion)
     */
    fun getLatestPresion(idUsuario: Int? = null): RegistrosSaludModelos.PresionRecord? {
        val db = readableDatabase
        val selection = idUsuario?.let { "id_usuario = ?" }
        val selectionArgs = idUsuario?.let { arrayOf(it.toString()) }

        val cursor = db.query(
            "presion_table",
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion"),
            selection,
            selectionArgs,
            null,
            null,
            "id_presion DESC",
            "1"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                val idLong = c.getLongOrNull("id_presion") ?: return null
                val idUsuarioVal = c.getIntOrNull("id_usuario")
                val sist = c.getDoubleOrNull("sistolica")
                val dias = c.getDoubleOrNull("diastolica")
                val fecha = c.getStringOrNull("fecha_registro")
                val fechaSync = c.getStringOrNull("fecha_sincronizacion")

                return RegistrosSaludModelos.PresionRecord(
                    id = idLong,
                    idUsuario = idUsuarioVal,
                    sistolica = sist,
                    diastolica = dias,
                    fecha_registro = fecha,
                    fecha_sincronizacion = fechaSync
                )
            }
        }
        return null
    }

    // -------------------------
    // Get all / pending lists
    // -------------------------

    /**
     * Return all glucosa rows for the given user (newest first).
     * Pass idUsuario = null to fetch all users.
     */
    fun getAllGlucosa(idUsuario: Int? = 1): List<RegistrosSaludModelos.GlucosaRecord> {
        val results = mutableListOf<RegistrosSaludModelos.GlucosaRecord>()
        val db = readableDatabase

        val selection = idUsuario?.let { "id_usuario = ?" }
        val selectionArgs = idUsuario?.let { arrayOf(it.toString()) }

        val cursor = db.query(
            "glucosa_table",
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "ayuno", "fecha_registro", "fecha_sincronizacion"),
            selection,
            selectionArgs,
            null,
            null,
            "id_glucosa DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    val idLong = c.getLongOrNull("id_glucosa") ?: continue
                    val idUsuarioVal = c.getIntOrNull("id_usuario")
                    val valor = c.getDoubleOrNull("valor_mgdl")
                    val ayunoVal = c.getIntOrNull("ayuno")
                    val fecha = c.getStringOrNull("fecha_registro")
                    val fechaSync = c.getStringOrNull("fecha_sincronizacion")

                    results.add(
                        RegistrosSaludModelos.GlucosaRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            valorMgdl = valor,
                            ayuno = ayunoVal,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync
                        )
                    )
                } while (c.moveToNext())
            }
        }

        return results
    }

    /**
     * Return all presion rows for the given user (newest first).
     * Pass idUsuario = null to fetch all users.
     */
    fun getAllPresion(idUsuario: Int? = 1): List<RegistrosSaludModelos.PresionRecord> {
        val results = mutableListOf<RegistrosSaludModelos.PresionRecord>()
        val db = readableDatabase

        val selection = idUsuario?.let { "id_usuario = ?" }
        val selectionArgs = idUsuario?.let { arrayOf(it.toString()) }

        val cursor = db.query(
            "presion_table",
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion"),
            selection,
            selectionArgs,
            null,
            null,
            "id_presion DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    val idLong = c.getLongOrNull("id_presion") ?: continue
                    val idUsuarioVal = c.getIntOrNull("id_usuario")
                    val sist = c.getDoubleOrNull("sistolica")
                    val dias = c.getDoubleOrNull("diastolica")
                    val fecha = c.getStringOrNull("fecha_registro")
                    val fechaSync = c.getStringOrNull("fecha_sincronizacion")

                    results.add(
                        RegistrosSaludModelos.PresionRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            sistolica = sist,
                            diastolica = dias,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync
                        )
                    )
                } while (c.moveToNext())
            }
        }

        return results
    }

    // -------------------------
    // Pending (unsynced) getters
    // -------------------------

    /**
     * Return pending (unsent) glucosa rows for the given user.
     * Pending rows are those with fecha_sincronizacion IS NULL or empty.
     */
    fun getPendingGlucosa(idUsuario: Int? = 1): List<RegistrosSaludModelos.GlucosaRecord> {
        val results = mutableListOf<RegistrosSaludModelos.GlucosaRecord>()
        val db = readableDatabase

        val selectionParts = mutableListOf<String>("(fecha_sincronizacion IS NULL OR fecha_sincronizacion = '')")
        val selectionArgsList = mutableListOf<String>()
        if (idUsuario != null) {
            selectionParts.add("id_usuario = ?")
            selectionArgsList.add(idUsuario.toString())
        }
        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null

        val cursor = db.query(
            "glucosa_table",
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "ayuno", "fecha_registro", "fecha_sincronizacion"),
            selection,
            selectionArgs,
            null,
            null,
            "id_glucosa DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    val idLong = c.getLongOrNull("id_glucosa") ?: continue
                    val idUsuarioVal = c.getIntOrNull("id_usuario")
                    val valor = c.getDoubleOrNull("valor_mgdl")
                    val ayunoVal = c.getIntOrNull("ayuno")
                    val fecha = c.getStringOrNull("fecha_registro")
                    val fechaSync = c.getStringOrNull("fecha_sincronizacion")

                    results.add(
                        RegistrosSaludModelos.GlucosaRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            valorMgdl = valor,
                            ayuno = ayunoVal,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync
                        )
                    )
                } while (c.moveToNext())
            }
        }
        return results
    }

    /**
     * Return pending (unsent) presion rows for the given user.
     */
    fun getPendingPresion(idUsuario: Int? = 1): List<RegistrosSaludModelos.PresionRecord> {
        val results = mutableListOf<RegistrosSaludModelos.PresionRecord>()
        val db = readableDatabase

        val selectionParts = mutableListOf<String>("(fecha_sincronizacion IS NULL OR fecha_sincronizacion = '')")
        val selectionArgsList = mutableListOf<String>()
        if (idUsuario != null) {
            selectionParts.add("id_usuario = ?")
            selectionArgsList.add(idUsuario.toString())
        }
        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null

        val cursor = db.query(
            "presion_table",
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion"),
            selection,
            selectionArgs,
            null,
            null,
            "id_presion DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    val idLong = c.getLongOrNull("id_presion") ?: continue
                    val idUsuarioVal = c.getIntOrNull("id_usuario")
                    val sist = c.getDoubleOrNull("sistolica")
                    val dias = c.getDoubleOrNull("diastolica")
                    val fecha = c.getStringOrNull("fecha_registro")
                    val fechaSync = c.getStringOrNull("fecha_sincronizacion")

                    results.add(
                        RegistrosSaludModelos.PresionRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            sistolica = sist,
                            diastolica = dias,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync
                        )
                    )
                } while (c.moveToNext())
            }
        }
        return results
    }

    // -------------------------
    // Mark sent (set fecha_sincronizacion)
    // -------------------------

    /**
     * Mark given glucosa ids as sent by setting fecha_sincronizacion to current datetime.
     */
    fun markGlucosaSent(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = defaultDateTimeFormat.format(Date())
            val args = ids.joinToString(",")
            val sql = "UPDATE glucosa_table SET fecha_sincronizacion = ? WHERE id_glucosa IN ($args)"
            db.compileStatement(sql).apply {
                bindString(1, now)
                execute()
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Mark given presion ids as sent by setting fecha_sincronizacion to current datetime.
     */
    fun markPresionSent(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = defaultDateTimeFormat.format(Date())
            val args = ids.joinToString(",")
            val sql = "UPDATE presion_table SET fecha_sincronizacion = ? WHERE id_presion IN ($args)"
            db.compileStatement(sql).apply {
                bindString(1, now)
                execute()
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun updateGlucosaSyncTimestamps(timestamps: Map<Long, String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues()
            for ((id, fecha) in timestamps) {
                cv.clear()
                cv.put("fecha_sincronizacion", fecha)
                db.update("glucosa_table", cv, "id_glucosa = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Update fecha_sincronizacion for presion rows.
     * timestamps: Map<id_presion, fecha_sincronizacion_iso>
     */
    fun updatePresionSyncTimestamps(timestamps: Map<Long, String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues()
            for ((id, fecha) in timestamps) {
                cv.clear()
                cv.put("fecha_sincronizacion", fecha)
                db.update("presion_table", cv, "id_presion = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

/* Cursor helper extension functions */
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