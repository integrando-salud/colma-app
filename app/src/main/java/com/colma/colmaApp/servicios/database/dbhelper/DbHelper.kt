package com.colma.colmaApp.servicios.database.dbhelper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.colma.colmaApp.servicios.Modelos.EstadoAnimoModelo
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
     * - fechaRegistro optional (defaults to now as "yyyy-MM-dd HH:mm:ss")
     * - fechaHoraEdicion optional (ISO string). If null we store current ISO datetime.
     */
    fun insertGlucosa(
        valorMgdl: Double,
        idUsuario: Int? = null,
        ayuno: Boolean? = null,
        fechaRegistro: String? = null,
        fechaHoraEdicion: String? = null // NEW: ISO string "yyyy-MM-dd HH:mm:ss"
    ): Long {
        val db: SQLiteDatabase = writableDatabase
        val values = ContentValues().apply {
            put("valor_mgdl", valorMgdl)
            idUsuario?.let { put("id_usuario", it) }
            put("fecha_registro", fechaRegistro ?: defaultDateTimeFormat.format(Date()))
            ayuno?.let { put("ayuno", if (it) 1 else 0) }
            // write edit datetime as ISO text
            put("fecha_hora_edicion", fechaHoraEdicion ?: defaultDateTimeFormat.format(Date()))
        }

        var rowId: Long = -1L
        db.beginTransaction()
        try {
            rowId = db.insert("glucosa_table", null, values)
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
     * - fechaRegistro optional (defaults to now)
     * - fechaHoraEdicionMillis optional (epoch millis). If null we store currentTimeMillis()
     */
    fun insertPresion(
        sistolica: Double,
        diastolica: Double,
        idUsuario: Int? = null,
        fechaRegistro: String? = null,
        fechaHoraEdicionIso: String? = null // NEW: ISO string
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("sistolica", sistolica)
            put("diastolica", diastolica)
            idUsuario?.let { put("id_usuario", it) }
            put("fecha_registro", fechaRegistro ?: defaultDateTimeFormat.format(Date()))
            // store the edit timestamp as ISO TEXT (matches glucosa behaviour)
            put("fecha_hora_edicion", fechaHoraEdicionIso ?: defaultDateTimeFormat.format(Date()))
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
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "fecha_registro", "ayuno", "fecha_sincronizacion", "fecha_hora_edicion"),
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
                val ayunoInt: Int? = c.getIntOrNull("ayuno")
                val fechaSync = c.getStringOrNull("fecha_sincronizacion")
                val fechaHoraEdicion = c.getStringOrNull("fecha_hora_edicion") // TEXT

                return RegistrosSaludModelos.GlucosaRecord(
                    id = idLong,
                    idUsuario = idUsuarioVal,
                    valorMgdl = valor,
                    ayuno = ayunoInt,
                    fecha_registro = fecha,
                    fecha_sincronizacion = fechaSync,
                    fecha_hora_edicion = fechaHoraEdicion
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
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion"),
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
                val fechaHoraEdicionMillis = c.getStringOrNull("fecha_hora_edicion") // INTEGER -> Long?

                return RegistrosSaludModelos.PresionRecord(
                    id = idLong,
                    idUsuario = idUsuarioVal,
                    sistolica = sist,
                    diastolica = dias,
                    fecha_registro = fecha,
                    fecha_sincronizacion = fechaSync,
                    fecha_hora_edicion = fechaHoraEdicionMillis
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
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "ayuno", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion"),
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
                    val fechaHoraEdicion = c.getStringOrNull("fecha_hora_edicion")

                    results.add(
                        RegistrosSaludModelos.GlucosaRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            valorMgdl = valor,
                            ayuno = ayunoVal,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync,
                            fecha_hora_edicion = fechaHoraEdicion
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
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion"),
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
                    val fechaHoraEdicionMillis = c.getStringOrNull("fecha_hora_edicion")

                    results.add(
                        RegistrosSaludModelos.PresionRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            sistolica = sist,
                            diastolica = dias,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync,
                            fecha_hora_edicion = fechaHoraEdicionMillis
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
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "ayuno", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion"),
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
                    val fechaHoraEdicion = c.getStringOrNull("fecha_hora_edicion")

                    results.add(
                        RegistrosSaludModelos.GlucosaRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            valorMgdl = valor,
                            ayuno = ayunoVal,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync,
                            fecha_hora_edicion = fechaHoraEdicion
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
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion"),
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
                    val fechaHoraEdicionMillis = c.getStringOrNull("fecha_hora_edicion")

                    results.add(
                        RegistrosSaludModelos.PresionRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            sistolica = sist,
                            diastolica = dias,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync,
                            fecha_hora_edicion = fechaHoraEdicionMillis
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

    fun getPendingEstadoAnimo(idPaciente: Int? = 1): EstadoAnimoModelo.EstadoAnimoRecord? {
        val db = readableDatabase
        val selectionParts = mutableListOf<String>("(fecha_sincronizacion IS NULL OR fecha_sincronizacion = '')")
        val selectionArgsList = mutableListOf<String>()
        if (idPaciente != null) {
            selectionParts.add("id_paciente = ?")
            selectionArgsList.add(idPaciente.toString())
        }
        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null

        val cols = arrayOf(
            "rowid",
            "id_paciente",
            "fecha_registro",
            "fecha_edicion",             // <-- include new column
            "pregunta_1","pregunta_2","pregunta_3","pregunta_4","pregunta_5",
            "pregunta_6","pregunta_7","pregunta_8","pregunta_9",
            "fecha_sincronizacion"
        )

        val cursor = db.query(
            "estado_animo_table",
            cols,
            selection,
            selectionArgs,
            null,
            null,
            "rowid DESC",
            "1"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                val rowid = c.getLongOrNull("rowid") ?: return null
                val idPac = c.getIntOrNull("id_paciente")
                val fechaReg = c.getStringOrNull("fecha_registro")
                val fechaEd = c.getStringOrNull("fecha_edicion")            // NEW: read fecha_edicion
                val preguntas = (1..9).map { idx -> c.getIntOrNull("pregunta_$idx") }
                val fechaSync = c.getStringOrNull("fecha_sincronizacion")
                return EstadoAnimoModelo.EstadoAnimoRecord(
                    rowid,
                    idPac,
                    fechaReg,
                    fechaEd,
                    preguntas,
                    fechaSync
                )
            }
        }
        return null
    }
    /**
     * Insert a new estado_animo row. preguntasMap keys are 1..9 (question index).
     * Any question not provided will be inserted as NULL.
     * fechaRegistro defaults to current datetime.
     */
    fun insertEstadoAnimo(
        idPaciente: Int,
        preguntasMap: Map<Int, Int?>,
        fechaRegistro: String? = null,
        fechaEdicion: String? = null // NEW: ISO string "yyyy-MM-dd HH:mm:ss"
    ): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id_paciente", idPaciente)
            put("fecha_registro", fechaRegistro ?: defaultDateTimeFormat.format(Date()))
            // write fecha_edicion text (user-edited datetime). default to now if not given.
            put("fecha_edicion", fechaEdicion ?: defaultDateTimeFormat.format(Date()))
            for (i in 1..9) {
                val key = "pregunta_$i"
                if (preguntasMap.containsKey(i)) {
                    val v = preguntasMap[i]
                    if (v == null) putNull(key) else put(key, v)
                } else {
                    // leave null
                }
            }
            // fecha_sincronizacion left null initially
        }

        var rowId: Long = -1L
        db.beginTransaction()
        try {
            rowId = db.insert("estado_animo_table", null, cv)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return rowId
    }
    /**
     * Update an existing estado_animo row by rowid, updating only provided questions.
     * preguntasMap keys are 1..9.
     */
    fun updateEstadoAnimoRow(rowid: Long, preguntasMap: Map<Int, Int?>, fechaEdicion: String? = null) {
        if (preguntasMap.isEmpty() && fechaEdicion == null) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues()
            for ((k, v) in preguntasMap) {
                cv.put("pregunta_$k", v)
            }
            if (fechaEdicion != null) {
                cv.put("fecha_edicion", fechaEdicion)
            }
            db.update("estado_animo_table", cv, "rowid = ?", arrayOf(rowid.toString()))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }
    /**
     * Convenience upsert: update the pending row for idPaciente if exists, otherwise insert.
     * Returns the rowid of the pending/created row.
     */
    fun upsertPendingEstadoAnimo(idPaciente: Int, preguntasMap: Map<Int, Int?>, fechaRegistro: String? = null, fechaEdicion: String? = null): Long {
        val pending = getPendingEstadoAnimo(idPaciente)
        return if (pending != null) {
            // update only provided questions (and fechaEdicion if provided)
            updateEstadoAnimoRow(pending.rowid, preguntasMap, fechaEdicion)
            pending.rowid
        } else {
            insertEstadoAnimo(idPaciente, preguntasMap, fechaRegistro, fechaEdicion)
        }
    }

    /**
     * Full update: set all pregunta_1..pregunta_9 fields for the pending row if exists,
     * otherwise insert a new full row.
     */
    fun upsertPendingEstadoAnimoFull(idPaciente: Int, fullPreguntas: Map<Int, Int?>, fechaRegistro: String? = null, fechaEdicion: String? = null): Long {
        val pending = getPendingEstadoAnimo(idPaciente)
        if (pending != null) {
            // create ContentValues for all fields and replace them
            val db = writableDatabase
            db.beginTransaction()
            try {
                val cv = ContentValues()
                for (i in 1..9) {
                    cv.put("pregunta_$i", fullPreguntas[i])
                }
                if (fechaEdicion != null) {
                    cv.put("fecha_edicion", fechaEdicion)
                }
                db.update("estado_animo_table", cv, "rowid = ?", arrayOf(pending.rowid.toString()))
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
            }
            return pending.rowid
        } else {
            return insertEstadoAnimo(idPaciente, fullPreguntas, fechaRegistro, fechaEdicion)
        }
    }

    fun getPendingEstadoAnimoList(idPaciente: Int? = 1): List<EstadoAnimoModelo.EstadoAnimoRecord> {
        val results = mutableListOf<EstadoAnimoModelo.EstadoAnimoRecord>()
        val db = readableDatabase

        val selectionParts = mutableListOf<String>("(fecha_sincronizacion IS NULL OR fecha_sincronizacion = '')")
        val selectionArgsList = mutableListOf<String>()
        if (idPaciente != null) {
            selectionParts.add("id_paciente = ?")
            selectionArgsList.add(idPaciente.toString())
        }
        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null

        val cols = arrayOf(
            "rowid",
            "id_paciente",
            "fecha_registro",
            "fecha_edicion",           // NEW
            "pregunta_1","pregunta_2","pregunta_3","pregunta_4","pregunta_5",
            "pregunta_6","pregunta_7","pregunta_8","pregunta_9",
            "fecha_sincronizacion"
        )

        val cursor = db.query(
            "estado_animo_table",
            cols,
            selection,
            selectionArgs,
            null,
            null,
            "rowid DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    val rowid = c.getLongOrNull("rowid") ?: continue
                    val idPac = c.getIntOrNull("id_paciente")
                    val fechaReg = c.getStringOrNull("fecha_registro")
                    val fechaEd = c.getStringOrNull("fecha_edicion")     // NEW
                    val preguntas = (1..9).map { idx -> c.getIntOrNull("pregunta_$idx") }
                    val fechaSync = c.getStringOrNull("fecha_sincronizacion")
                    results.add(
                        EstadoAnimoModelo.EstadoAnimoRecord(
                            rowid,
                            idPac,
                            fechaReg,
                            fechaEd,
                            preguntas,
                            fechaSync
                        )
                    )
                } while (c.moveToNext())
            }
        }
        return results
    }
    /**
     * Update fecha_sincronizacion for estado_animo rows.
     * timestamps: Map<rowid, fecha_sincronizacion_iso>
     */
    fun updateEstadoAnimoSyncTimestamps(timestamps: Map<Long, String>) {
        if (timestamps.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues()
            for ((id, fecha) in timestamps) {
                cv.clear()
                cv.put("fecha_sincronizacion", fecha)
                db.update("estado_animo_table", cv, "rowid = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllEstadoAnimo(idPaciente: Int? = 1): List<EstadoAnimoModelo.EstadoAnimoRecord> {
        val results = mutableListOf<EstadoAnimoModelo.EstadoAnimoRecord>()
        val db = readableDatabase

        val selection = idPaciente?.let { "id_paciente = ?" }
        val selectionArgs = idPaciente?.let { arrayOf(it.toString()) }

        // include fecha_edicion as requested
        val cols = arrayOf(
            "rowid",
            "id_paciente",
            "fecha_registro",
            "fecha_edicion",
            "pregunta_1","pregunta_2","pregunta_3","pregunta_4","pregunta_5",
            "pregunta_6","pregunta_7","pregunta_8","pregunta_9",
            "fecha_sincronizacion"
        )

        val cursor = db.query(
            "estado_animo_table",
            cols,
            selection,
            selectionArgs,
            null,
            null,
            "rowid DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    val rowid = c.getLongOrNull("rowid") ?: continue
                    val idPac = c.getIntOrNull("id_paciente")
                    val fechaReg = c.getStringOrNull("fecha_registro")
                    val fechaEd = c.getStringOrNull("fecha_edicion")   // read fecha_edicion
                    val preguntas = (1..9).map { idx -> c.getIntOrNull("pregunta_$idx") }
                    val fechaSync = c.getStringOrNull("fecha_sincronizacion")

                    results.add(
                        EstadoAnimoModelo.EstadoAnimoRecord(
                            rowid = rowid,
                            idPaciente = idPac,
                            fechaRegistro = fechaReg,
                            fechaEdicion = fechaEd,
                            preguntas = preguntas,
                            fechaSincronizacion = fechaSync
                        )
                    )
                } while (c.moveToNext())
            }
        }

        return results
    }

    fun insertSincronizacion(fecha: String, estado: String, data: String?): Long {
        val db = writableDatabase
        var rowId: Long = -1L
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("fecha_sincronizacion", fecha)
                put("estado", estado)
                data?.let { put("data", it) }
            }
            rowId = db.insert("sincronizacion_table", null, cv)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return rowId
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