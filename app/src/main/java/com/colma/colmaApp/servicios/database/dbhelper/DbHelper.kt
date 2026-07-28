package com.colma.colmaApp.servicios.database.dbhelper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.colma.colmaApp.servicios.Modelos.EstadoAnimoModelo
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord
import com.colma.colmaApp.servicios.internetCommunication.model.CentroSalud
import com.colma.colmaApp.servicios.internetCommunication.model.Pais
import com.colma.colmaApp.servicios.internetCommunication.model.Usuario
import com.colma.colmaApp.utils.EncryptionUtils
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DbHelper(context: Context) :
    SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val defaultDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val DATABASE_NAME = "colmadb.db"
        private const val DATABASE_VERSION = 13
    }

    data class CredencialLocal(
        val id: Int = 0,
        val documentoEncriptado: String,
        val passwordEncriptada: String,
        val tokenEncriptado: String,
        val idUsuario: Int
    )

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS credenciales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    documento_encriptado TEXT,
                    password_encriptada TEXT,
                    token_encriptado TEXT,
                    id_usuario INTEGER
                );
            """)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedCredentialForDni(dni: String): CredencialLocal? {
        val db = readableDatabase
        try {
            db.query(
                "credenciales",
                arrayOf("id", "documento_encriptado", "password_encriptada", "token_encriptado", "id_usuario"),
                null, null, null, null, null
            ).use { cursor ->
                val idIndex = cursor.getColumnIndex("id")
                val docIndex = cursor.getColumnIndex("documento_encriptado")
                val pwdIndex = cursor.getColumnIndex("password_encriptada")
                val tokenIndex = cursor.getColumnIndex("token_encriptado")
                val userIndex = cursor.getColumnIndex("id_usuario")

                if (idIndex != -1 && docIndex != -1 && pwdIndex != -1 && tokenIndex != -1 && userIndex != -1) {
                    while (cursor.moveToNext()) {
                        val encryptedDoc = cursor.getString(docIndex)
                        try {
                            val decryptedDoc = EncryptionUtils.decrypt(encryptedDoc)
                            if (decryptedDoc == dni) {
                                return CredencialLocal(
                                    id = cursor.getInt(idIndex),
                                    documentoEncriptado = encryptedDoc,
                                    passwordEncriptada = cursor.getString(pwdIndex),
                                    tokenEncriptado = cursor.getString(tokenIndex),
                                    idUsuario = cursor.getInt(userIndex)
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun saveOrUpdateCredential(dni: String, passwordRaw: String, tokenRaw: String, idUsuario: Int) {
        try {
            val existing = getSavedCredentialForDni(dni)
            val encryptedPassword = EncryptionUtils.encrypt(passwordRaw)
            val encryptedToken = EncryptionUtils.encrypt(tokenRaw)
            val encryptedDni = EncryptionUtils.encrypt(dni)

            val db = writableDatabase
            val values = ContentValues().apply {
                put("documento_encriptado", encryptedDni)
                put("password_encriptada", encryptedPassword)
                put("token_encriptado", encryptedToken)
                put("id_usuario", idUsuario)
            }

            if (existing != null) {
                try {
                    val decryptedPassword = EncryptionUtils.decrypt(existing.passwordEncriptada)
                    val decryptedToken = EncryptionUtils.decrypt(existing.tokenEncriptado)
                    if (decryptedPassword == passwordRaw && decryptedToken == tokenRaw && existing.idUsuario == idUsuario) {
                        return
                    }
                } catch (e: Exception) {
                    // Overwrite if decryption fails
                }
                db.update("credenciales", values, "id = ?", arrayOf(existing.id.toString()))
            } else {
                db.insert("credenciales", null, values)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun verifyCredentialOffline(dni: String, passwordRaw: String): Result<Pair<String, Int>> {
        return try {
            val credential = getSavedCredentialForDni(dni)
                ?: return Result.failure(Exception("Usuario no encontrado en la base de datos local"))
            val decryptedPassword = EncryptionUtils.decrypt(credential.passwordEncriptada)
            if (decryptedPassword == passwordRaw) {
                val decryptedToken = EncryptionUtils.decrypt(credential.tokenEncriptado)
                Result.success(Pair(decryptedToken, credential.idUsuario))
            } else {
                Result.failure(Exception("Contraseña incorrecta"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error al verificar credenciales: ${e.message}"))
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This onUpgrade is designed to be robust and fix schema issues from previous versions.

        // Step 1: Ensure metas_table exists. This fixes crashes for users where the table is missing.
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS metas_table (
                    id_meta INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_usuario INTEGER,
                    semana INTEGER,
                    sesion INTEGER,
                    tipo_actividad TEXT,
                    actividad TEXT,
                    lugar TEXT,
                    acompañante TEXT,
                    fecha_regisitro TEXT,
                    fecha_edicion TEXT,
                    fecha_sincronizacion TEXT,
                    meta_cumplida INTEGER,
                    fecha_objetivo TEXT
                );
            """)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step 2: Ensure the 'motivo_incumplimiento' column exists.
        // This handles both the original v2 migration and fixes broken v2 schemas.
        var columnExists = false
        try {
            db.rawQuery("PRAGMA table_info(metas_table)", null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if ("motivo_incumplimiento" == cursor.getString(nameIndex)) {
                            columnExists = true
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!columnExists) {
            try {
                db.execSQL("ALTER TABLE metas_table ADD COLUMN motivo_incumplimiento TEXT;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Step 3: Ensure the 'advertencia' column exists in glucosa_table.
        var advertenciaColumnExists = false
        try {
            db.rawQuery("PRAGMA table_info(glucosa_table)", null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if ("advertencia" == cursor.getString(nameIndex)) {
                            advertenciaColumnExists = true
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!advertenciaColumnExists) {
            try {
                db.execSQL("ALTER TABLE glucosa_table ADD COLUMN advertencia TEXT;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Step 4: Ensure the 'advertencia' column exists in presion_table.
        var advertenciaPresionColumnExists = false
        try {
            db.rawQuery("PRAGMA table_info(presion_table)", null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if ("advertencia" == cursor.getString(nameIndex)) {
                            advertenciaPresionColumnExists = true
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!advertenciaPresionColumnExists) {
            try {
                db.execSQL("ALTER TABLE presion_table ADD COLUMN advertencia TEXT;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Step 4.5: Ensure all tables exist before performing checks or updates
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS multimedia_read_table (
                    id_multimedia_read INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_usuario INTEGER,
                    treatment_id TEXT,
                    week_id INTEGER,
                    session_id INTEGER,
                    multimedia_type TEXT,
                    multimedia_unique_key TEXT,
                    fecha_registro TEXT,
                    fecha_sincronizacion TEXT
                );
            """)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS quiz_response_table (
                    id_quiz_response INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_usuario INTEGER,
                    quiz_type TEXT,
                    week_id INTEGER,
                    session_id INTEGER,
                    quiz_title TEXT,
                    fecha_registro TEXT,
                    fecha_sincronizacion TEXT,
                    total_questions INTEGER,
                    correct_answers INTEGER,
                    percentage_correct REAL,
                    multimedia_unique_key TEXT,
                    treatment_type TEXT
                );
            """)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS question_response_table (
                    id_question_response INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_quiz_response INTEGER,
                    question_index INTEGER,
                    question_text TEXT,
                    user_answer_index INTEGER,
                    user_answer_text TEXT,
                    correct_answer_index INTEGER,
                    correct_answer_text TEXT,
                    is_correct INTEGER,
                    posibles_respuestas TEXT,
                    FOREIGN KEY (id_quiz_response) REFERENCES quiz_response_table(id_quiz_response) ON DELETE CASCADE
                );
            """)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS paises (
                id INTEGER PRIMARY KEY,
                nombre TEXT
            );
        """)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS centros_salud (
                id INTEGER PRIMARY KEY,
                nombre TEXT
            );
        """)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step 5: Ensure 'treatment_type' column exists in 'quiz_response_table'
        var treatmentTypeColumnExists = false
        try {
            db.rawQuery("PRAGMA table_info(quiz_response_table)", null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if ("treatment_type" == cursor.getString(nameIndex)) {
                            treatmentTypeColumnExists = true
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!treatmentTypeColumnExists) {
            try {
                db.execSQL("ALTER TABLE quiz_response_table ADD COLUMN treatment_type TEXT;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Step 6: Ensure 'posibles_respuestas' column exists in 'question_response_table'
        var posiblesRespuestasColumnExists = false
        try {
            db.rawQuery("PRAGMA table_info(question_response_table)", null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if ("posibles_respuestas" == cursor.getString(nameIndex)) {
                            posiblesRespuestasColumnExists = true
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!posiblesRespuestasColumnExists) {
            try {
                db.execSQL("ALTER TABLE question_response_table ADD COLUMN posibles_respuestas TEXT;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Add new columns to 'usuarios' table
        val usuariosColumns = mutableSetOf<String>()
        try {
            db.rawQuery("PRAGMA table_info(usuarios)", null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        usuariosColumns.add(cursor.getString(nameIndex).lowercase(Locale.ROOT))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!usuariosColumns.contains("documento")) {
            try {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN documento TEXT;")
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (!usuariosColumns.contains("email")) {
            try {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN email TEXT;")
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (!usuariosColumns.contains("sexo")) {
            try {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN sexo TEXT;")
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (!usuariosColumns.contains("id_pais")) {
            try {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN id_pais INTEGER;")
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (!usuariosColumns.contains("id_centro_salud")) {
            try {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN id_centro_salud INTEGER;")
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (!usuariosColumns.contains("fecha_registro")) {
            try {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN fecha_registro TEXT;")
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (!usuariosColumns.contains("primer_login")) {
            try {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN primer_login TEXT;")
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

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
        fechaHoraEdicion: String? = null, // NEW: ISO string "yyyy-MM-dd HH:mm:ss"
        advertencia: String? = null
    ): Long {
        val db: SQLiteDatabase = writableDatabase
        val values = ContentValues().apply {
            put("valor_mgdl", valorMgdl)
            idUsuario?.let { put("id_usuario", it) }
            put("fecha_registro", fechaRegistro ?: defaultDateTimeFormat.format(Date()))
            ayuno?.let { put("ayuno", if (it) 1 else 0) }
            // write edit datetime as ISO text
            put("fecha_hora_edicion", fechaHoraEdicion ?: defaultDateTimeFormat.format(Date()))
            advertencia?.let { put("advertencia", it) }
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
        fechaHoraEdicionIso: String? = null, // NEW: ISO string
        advertencia: String? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("sistolica", sistolica)
            put("diastolica", diastolica)
            idUsuario?.let { put("id_usuario", it) }
            put("fecha_registro", fechaRegistro ?: defaultDateTimeFormat.format(Date()))
            // store the edit timestamp as ISO TEXT (matches glucosa behaviour)
            put("fecha_hora_edicion", fechaHoraEdicionIso ?: defaultDateTimeFormat.format(Date()))
            advertencia?.let { put("advertencia", it) }
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
    
    /**
     * Insert into multimedia_read_table
     * - id_usuario required
     * - treatment_id required
     * - week_id required
     * - session_id required
     * - multimedia_type required
     * - multimedia_unique_key required
     * - fecha_registro optional (defaults to now as "yyyy-MM-dd HH:mm:ss")
     * - fecha_sincronizacion optional (defaults to null)
     */
    fun insertMultimediaRead(
        idUsuario: Int,
        treatmentId: String,
        weekId: Int,
        sessionId: Int,
        multimediaType: String,
        multimediaUniqueKey: String,
        fechaRegistro: String? = null,
        fechaSincronizacion: String? = null
    ): Long {
        val db: SQLiteDatabase = writableDatabase
        val values = ContentValues().apply {
            put("id_usuario", idUsuario)
            put("treatment_id", treatmentId)
            put("week_id", weekId)
            put("session_id", sessionId)
            put("multimedia_type", multimediaType)
            put("multimedia_unique_key", multimediaUniqueKey)
            put("fecha_registro", fechaRegistro ?: defaultDateTimeFormat.format(Date()))
            fechaSincronizacion?.let { put("fecha_sincronizacion", it) } ?: putNull("fecha_sincronizacion")
        }

        var rowId: Long = -1L
        db.beginTransaction()
        try {
            rowId = db.insert("multimedia_read_table", null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return rowId
    }

    /**
     * Check if a MultimediaReadRecord already exists for the given unique identifiers.
     */
    fun getMultimediaReadRecordExists(
        idUsuario: Int,
        treatmentId: String,
        weekId: Int,
        sessionId: Int,
        multimediaType: String,
        multimediaUniqueKey: String
    ): Boolean {
        val db = readableDatabase
        val selection = """
            id_usuario = ? AND
            treatment_id = ? AND
            week_id = ? AND
            session_id = ? AND
            multimedia_type = ? AND
            multimedia_unique_key = ?
        """.trimIndent()
        val selectionArgs = arrayOf(
            idUsuario.toString(),
            treatmentId,
            weekId.toString(),
            sessionId.toString(),
            multimediaType,
            multimediaUniqueKey
        )

        var exists = false
        db.query(
            "multimedia_read_table",
            arrayOf("id_multimedia_read"),
            selection,
            selectionArgs,
            null, null, null
        ).use { cursor ->
            exists = cursor.count > 0
        }
        return exists
    }

    /**
     * Upsert a multimedia_read record. Inserts if not exists, otherwise does nothing.
     * Returns the rowId if a new record was inserted, or -1L if it already existed.
     */
    fun upsertMultimediaRead(
        idUsuario: Int,
        treatmentId: String,
        weekId: Int,
        sessionId: Int,
        multimediaType: String,
        multimediaUniqueKey: String
    ): Long {
        if (getMultimediaReadRecordExists(idUsuario, treatmentId, weekId, sessionId, multimediaType, multimediaUniqueKey)) {
            return -1L // Record already exists, do not insert
        }
        // Record does not exist, proceed with insertion
        return insertMultimediaRead(
            idUsuario,
            treatmentId,
            weekId,
            sessionId,
            multimediaType,
            multimediaUniqueKey
        )
    }

    /**
     * Insert a new quiz response and its associated question responses.
     * Returns the rowId of the inserted QuizResponseRecord, or -1L on failure.
     */
    fun insertQuizResponse(
        quizResponse: QuizResponseRecord,
        questionResponses: List<QuestionResponseRecord>
    ): Long {
        val db: SQLiteDatabase = writableDatabase
        var quizResponseId: Long = -1L
        db.beginTransaction()
        try {
            val quizValues = ContentValues().apply {
                put("id_usuario", quizResponse.id_usuario)
                put("quiz_type", quizResponse.quiz_type)
                put("week_id", quizResponse.week_id)
                put("session_id", quizResponse.session_id)
                put("quiz_title", quizResponse.quiz_title)
                put("fecha_registro", quizResponse.fecha_registro ?: defaultDateTimeFormat.format(Date()))
                if (quizResponse.fecha_sincronizacion != null) {
                    put("fecha_sincronizacion", quizResponse.fecha_sincronizacion)
                } else {
                    putNull("fecha_sincronizacion")
                }
                put("total_questions", quizResponse.total_questions)
                put("correct_answers", quizResponse.correct_answers)
                put("percentage_correct", quizResponse.percentage_correct)
                put("multimedia_unique_key", quizResponse.multimedia_unique_key)
                // treatment_type removed - quiz_type already contains hta/diabetes
            }
            quizResponseId = db.insert("quiz_response_table", null, quizValues)

            if (quizResponseId != -1L) {
                for (question in questionResponses) {
                    val questionValues = ContentValues().apply {
                        put("id_quiz_response", quizResponseId)
                        put("question_index", question.question_index)
                        put("question_text", question.question_text)
                        put("user_answer_index", question.user_answer_index)
                        put("user_answer_text", question.user_answer_text)
                        put("correct_answer_index", question.correct_answer_index)
                        put("correct_answer_text", question.correct_answer_text)
                        put("is_correct", question.is_correct)
                        put("posibles_respuestas", question.posibles_respuestas)
                    }
                    db.insert("question_response_table", null, questionValues)
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return quizResponseId
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
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "fecha_registro", "ayuno", "fecha_sincronizacion", "fecha_hora_edicion", "advertencia"),
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
                val advertencia = c.getStringOrNull("advertencia")

                return RegistrosSaludModelos.GlucosaRecord(
                    id = idLong,
                    idUsuario = idUsuarioVal,
                    valorMgdl = valor,
                    ayuno = ayunoInt,
                    fecha_registro = fecha,
                    fecha_sincronizacion = fechaSync,
                    fecha_hora_edicion = fechaHoraEdicion,
                    advertencia = advertencia
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
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion", "advertencia"),
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
                val advertencia = c.getStringOrNull("advertencia")

                return RegistrosSaludModelos.PresionRecord(
                    id = idLong,
                    idUsuario = idUsuarioVal,
                    sistolica = sist,
                    diastolica = dias,
                    fecha_registro = fecha,
                    fecha_sincronizacion = fechaSync,
                    fecha_hora_edicion = fechaHoraEdicionMillis,
                    advertencia = advertencia
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
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "ayuno", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion", "advertencia"),
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
                    val advertencia = c.getStringOrNull("advertencia")

                    results.add(
                        RegistrosSaludModelos.GlucosaRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            valorMgdl = valor,
                            ayuno = ayunoVal,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync,
                            fecha_hora_edicion = fechaHoraEdicion,
                            advertencia = advertencia
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
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion", "advertencia"),
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
                    val advertencia = c.getStringOrNull("advertencia")

                    results.add(
                        RegistrosSaludModelos.PresionRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            sistolica = sist,
                            diastolica = dias,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync,
                            fecha_hora_edicion = fechaHoraEdicionMillis,
                            advertencia = advertencia
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
            arrayOf("id_glucosa", "id_usuario", "valor_mgdl", "ayuno", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion", "advertencia"),
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
                    val advertencia = c.getStringOrNull("advertencia")

                    results.add(
                        RegistrosSaludModelos.GlucosaRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            valorMgdl = valor,
                            ayuno = ayunoVal,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync,
                            fecha_hora_edicion = fechaHoraEdicion,
                            advertencia = advertencia
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
            arrayOf("id_presion", "id_usuario", "sistolica", "diastolica", "fecha_registro", "fecha_sincronizacion", "fecha_hora_edicion", "advertencia"),
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
                    val advertencia = c.getStringOrNull("advertencia")

                    results.add(
                        RegistrosSaludModelos.PresionRecord(
                            id = idLong,
                            idUsuario = idUsuarioVal,
                            sistolica = sist,
                            diastolica = dias,
                            fecha_registro = fecha,
                            fecha_sincronizacion = fechaSync,
                            fecha_hora_edicion = fechaHoraEdicionMillis,
                            advertencia = advertencia
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

    fun getEstadoAnimoByRowId(rowid: Long): EstadoAnimoModelo.EstadoAnimoRecord? {
        val db = readableDatabase
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
            "rowid = ?",
            arrayOf(rowid.toString()),
            null, null, null
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                val idPac = c.getIntOrNull("id_paciente")
                val fechaReg = c.getStringOrNull("fecha_registro")
                val fechaEd = c.getStringOrNull("fecha_edicion")
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

    fun deleteEstadoAnimoRow(rowid: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("estado_animo_table", "rowid = ?", arrayOf(rowid.toString()))
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

    /**
     * Get the date of the most recent mood record for a user.
     * Returns the fecha_edicion date as a String in "yyyy-MM-dd HH:mm:ss" format, or null if no records exist.
     */
    fun getLatestEstadoAnimoDate(idPaciente: Int): String? {
        val db = readableDatabase
        val selection = "id_paciente = ?"
        val selectionArgs = arrayOf(idPaciente.toString())

        val cursor = db.query(
            "estado_animo_table",
            arrayOf("fecha_edicion"),
            selection,
            selectionArgs,
            null,
            null,
            "rowid DESC",
            "1"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getStringOrNull("fecha_edicion")
            }
        }
        return null
    }

    fun getAllMultimediaRead(idUsuario: Int? = 1): List<RegistrosSaludModelos.MultimediaReadRecord> {
        val results = mutableListOf<RegistrosSaludModelos.MultimediaReadRecord>()
        val db = readableDatabase

        val selection = idUsuario?.let { "id_usuario = ?" }
        val selectionArgs = idUsuario?.let { arrayOf(it.toString()) }

        val cursor = db.query(
            "multimedia_read_table",
            null, // All columns
            selection,
            selectionArgs,
            null,
            null,
            "id_multimedia_read DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    results.add(cursorToMultimediaReadRecord(c))
                } while (c.moveToNext())
            }
        }
        return results
    }

    /**
     * Get the count of unique multimedia items read for a specific user, treatment, week, and subsession.
     */
    fun getReadMultimediaCount(idUsuario: Int, treatmentId: String, weekId: Int, sessionId: Int): Int {
        val db = readableDatabase
        val selection = "id_usuario = ? AND treatment_id = ? AND week_id = ? AND session_id = ?"
        val selectionArgs = arrayOf(idUsuario.toString(), treatmentId, weekId.toString(), sessionId.toString())

        var count = 0
        db.rawQuery("SELECT COUNT(*) FROM multimedia_read_table WHERE $selection", selectionArgs).use { cursor ->
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
        }
        return count
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

    // Metas Table Methods

    fun insertMeta(meta: RegistrosSaludModelos.MetaRecord): Long {
        val db = writableDatabase

        // Check if there is already an active goal (meta_cumplida = 0) for this user
        val idUser = meta.id_usuario ?: 1
        val cursor = db.rawQuery(
            "SELECT 1 FROM metas_table WHERE id_usuario = ? AND meta_cumplida = 0 LIMIT 1",
            arrayOf(idUser.toString())
        )
        val hasActive = cursor.use { it.moveToFirst() }
        if (hasActive) return -1L

        val values = ContentValues().apply {
            put("id_usuario", meta.id_usuario)
            put("semana", meta.semana)
            put("sesion", meta.sesion)
            put("tipo_actividad", meta.tipo_actividad)
            put("actividad", meta.actividad)
            put("lugar", meta.lugar)
            put("acompañante", meta.acompañante)
            put("fecha_regisitro", meta.fecha_regisitro ?: defaultDateTimeFormat.format(Date()))
            put("fecha_edicion", meta.fecha_edicion ?: defaultDateTimeFormat.format(Date()))
            put("meta_cumplida", meta.meta_cumplida)
            put("fecha_objetivo", meta.fecha_objetivo)
            put("motivo_incumplimiento", meta.motivo_incumplimiento)
        }

        var rowId: Long = -1L
        db.beginTransaction()
        try {
            rowId = db.insert("metas_table", null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return rowId
    }

    fun updateMeta(meta: RegistrosSaludModelos.MetaRecord): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id_usuario", meta.id_usuario)
            put("semana", meta.semana)
            put("sesion", meta.sesion)
            put("tipo_actividad", meta.tipo_actividad)
            put("actividad", meta.actividad)
            put("lugar", meta.lugar)
            put("acompañante", meta.acompañante)
            put("fecha_edicion", defaultDateTimeFormat.format(Date()))
            put("meta_cumplida", meta.meta_cumplida)
            put("fecha_objetivo", meta.fecha_objetivo)
            put("motivo_incumplimiento", meta.motivo_incumplimiento)
        }

        var affectedRows = 0
        db.beginTransaction()
        try {
            affectedRows = db.update("metas_table", values, "id_meta = ?", arrayOf(meta.id_meta.toString()))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return affectedRows
    }


    fun deleteMeta(id: Long): Int {
        val db = writableDatabase
        var affectedRows = 0
        db.beginTransaction()
        try {
            affectedRows = db.delete("metas_table", "id_meta = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return affectedRows
    }

    fun getMetaById(id: Long): RegistrosSaludModelos.MetaRecord? {
        val db = readableDatabase
        val cursor = db.query(
            "metas_table",
            null,
            "id_meta = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToMetaRecord(c)
            }
        }
        return null
    }

    fun getAllMetas(idUsuario: Int? = null): List<RegistrosSaludModelos.MetaRecord> {
        val results = mutableListOf<RegistrosSaludModelos.MetaRecord>()
        val db = readableDatabase
        val selection = idUsuario?.let { "id_usuario = ?" }
        val selectionArgs = idUsuario?.let { arrayOf(it.toString()) }

        val cursor = db.query(
            "metas_table",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "id_meta DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    results.add(cursorToMetaRecord(c))
                } while (c.moveToNext())
            }
        }
        return results
    }

    fun getMetaForToday(idUsuario: Int? = 1): RegistrosSaludModelos.MetaRecord? {
        val db = readableDatabase
        val selectionParts = mutableListOf<String>("date(fecha_objetivo) = date('now', 'localtime')")
        val selectionArgsList = mutableListOf<String>()

        if (idUsuario != null) {
            selectionParts.add("id_usuario = ?")
            selectionArgsList.add(idUsuario.toString())
        }

        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null

        val cursor = db.query(
            "metas_table",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "id_meta DESC",
            "1"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToMetaRecord(c)
            }
        }
        return null
    }

    fun getPendingFutureMetasCount(idUsuario: Int? = 1): Int {
        val db = readableDatabase
        val selectionParts = mutableListOf<String>("meta_cumplida = 0", "date(fecha_objetivo) >= date('now', 'localtime')")
        val selectionArgsList = mutableListOf<String>()

        if (idUsuario != null) {
            selectionParts.add("id_usuario = ?")
            selectionArgsList.add(idUsuario.toString())
        }

        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null

        val cursor = db.rawQuery("SELECT COUNT(*) FROM metas_table WHERE $selection", selectionArgs)

        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getInt(0)
            }
        }
        return 0
    }

    /**
     * Returns the highest week number for which the user has created a goal,
     * filtered by treatment type (hta/diabetes).
     * Returns -1 if no goals exist for this user and treatment type.
     */
    fun getHighestWeekWithGoal(idUsuario: Int, treatmentType: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT MAX(semana) FROM metas_table WHERE id_usuario = ? AND tipo_actividad = ?",
            arrayOf(idUsuario.toString(), treatmentType)
        )
        
        cursor.use { c ->
            if (c.moveToFirst()) {
                val week = c.getInt(0)
                return if (week > 0) week else -1
            }
        }
        return -1
    }

    /**
     * Returns true if the user has already completed a goal for the specified week and treatment type.
     */
    fun hasCompletedGoalForWeek(idUsuario: Int, semana: Int, treatmentType: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM metas_table WHERE id_usuario = ? AND semana = ? AND tipo_actividad = ? AND meta_cumplida = 2 LIMIT 1",
            arrayOf(idUsuario.toString(), semana.toString(), treatmentType)
        )
        return cursor.use { c -> c.moveToFirst() }
    }

    fun getPendingPastMeta(idUsuario: Int? = 1): RegistrosSaludModelos.MetaRecord? {
        val db = readableDatabase
        val selectionParts = mutableListOf<String>("meta_cumplida = 0", "date(fecha_objetivo) <= date('now', 'localtime')")
        val selectionArgsList = mutableListOf<String>()

        if (idUsuario != null) {
            selectionParts.add("id_usuario = ?")
            selectionArgsList.add(idUsuario.toString())
        }

        val selection = selectionParts.joinToString(" AND ")
        val selectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null

        val cursor = db.query(
            "metas_table",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "fecha_objetivo ASC", // Get the oldest one first
            "1"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                return cursorToMetaRecord(c)
            }
        }
        return null
    }

    fun getPendingMetas(idUsuario: Int? = 1): List<RegistrosSaludModelos.MetaRecord> {
        val results = mutableListOf<RegistrosSaludModelos.MetaRecord>()
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
            "metas_table",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "id_meta DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    results.add(cursorToMetaRecord(c))
                } while (c.moveToNext())
            }
        }
        return results
    }

    /**
     * Return pending (unsent) multimedia_read rows for the given user.
     * Pending rows are those with fecha_sincronizacion IS NULL or empty.
     */
    fun getPendingMultimediaRead(idUsuario: Int? = 1): List<RegistrosSaludModelos.MultimediaReadRecord> {
        val results = mutableListOf<RegistrosSaludModelos.MultimediaReadRecord>()
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
            "multimedia_read_table",
            null, // All columns
            selection,
            selectionArgs,
            null,
            null,
            "id_multimedia_read DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    results.add(cursorToMultimediaReadRecord(c))
                } while (c.moveToNext())
            }
        }
        return results
    }

    /**
     * Return pending (unsent) quiz responses for the given user.
     * Pending rows are those with fecha_sincronizacion IS NULL or empty.
     */
    fun getPendingQuizResponses(idUsuario: Int? = 1): List<QuizResponseRecord> {
        val results = mutableListOf<QuizResponseRecord>()
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
            "quiz_response_table",
            null, // All columns
            selection,
            selectionArgs,
            null,
            null,
            "id_quiz_response DESC"
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    results.add(cursorToQuizResponseRecord(c))
                } while (c.moveToNext())
            }
        }
        return results
    }


    fun markMetasSent(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = defaultDateTimeFormat.format(Date())
            val args = ids.joinToString(",")
            val sql = "UPDATE metas_table SET fecha_sincronizacion = ? WHERE id_meta IN ($args)"
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

    fun markMultimediaReadSent(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = defaultDateTimeFormat.format(Date())
            val args = ids.joinToString(",")
            val sql = "UPDATE multimedia_read_table SET fecha_sincronizacion = ? WHERE id_multimedia_read IN ($args)"
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
     * Mark given quiz_response ids as sent by setting fecha_sincronizacion to current datetime.
     */
    fun markQuizResponsesSent(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = defaultDateTimeFormat.format(Date())
            val args = ids.joinToString(",")
            val sql = "UPDATE quiz_response_table SET fecha_sincronizacion = ? WHERE id_quiz_response IN ($args)"
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

    fun getUncompletedFutureMetas(): List<RegistrosSaludModelos.MetaRecord> {
        val results = mutableListOf<RegistrosSaludModelos.MetaRecord>()
        val db = readableDatabase
        val cursor = db.query(
            "metas_table",
            null,
            "meta_cumplida = 0",
            null,
            null,
            null,
            "id_meta ASC"
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    results.add(cursorToMetaRecord(c))
                } while (c.moveToNext())
            }
        }
        return results
    }

    private fun cursorToMetaRecord(c: Cursor): RegistrosSaludModelos.MetaRecord {
        return RegistrosSaludModelos.MetaRecord(
            id_meta = c.getLongOrNull("id_meta") ?: -1L,
            id_usuario = c.getIntOrNull("id_usuario"),
            semana = c.getIntOrNull("semana"),
            sesion = c.getIntOrNull("sesion"),
            tipo_actividad = c.getStringOrNull("tipo_actividad"),
            actividad = c.getStringOrNull("actividad"),
            lugar = c.getStringOrNull("lugar"),
            acompañante = c.getStringOrNull("acompañante"),
            fecha_regisitro = c.getStringOrNull("fecha_regisitro"),
            fecha_edicion = c.getStringOrNull("fecha_edicion"),
            fecha_sincronizacion = c.getStringOrNull("fecha_sincronizacion"),
            meta_cumplida = c.getIntOrNull("meta_cumplida"),
            fecha_objetivo = c.getStringOrNull("fecha_objetivo"),
            motivo_incumplimiento = c.getStringOrNull("motivo_incumplimiento")
        )
    }

    private fun cursorToMultimediaReadRecord(c: Cursor): RegistrosSaludModelos.MultimediaReadRecord {
        return RegistrosSaludModelos.MultimediaReadRecord(
            id_multimedia_read = c.getLongOrNull("id_multimedia_read") ?: -1L,
            id_usuario = c.getIntOrNull("id_usuario"),
            treatment_id = c.getStringOrNull("treatment_id"),
            week_id = c.getIntOrNull("week_id"),
            session_id = c.getIntOrNull("session_id"),
            multimedia_type = c.getStringOrNull("multimedia_type"),
            multimedia_unique_key = c.getStringOrNull("multimedia_unique_key"),
            fecha_registro = c.getStringOrNull("fecha_registro"),
            fecha_sincronizacion = c.getStringOrNull("fecha_sincronizacion")
        )
    }

    private fun cursorToQuizResponseRecord(c: Cursor): QuizResponseRecord {
        return QuizResponseRecord(
            id_quiz_response = c.getLongOrNull("id_quiz_response") ?: -1L,
            id_usuario = c.getIntOrNull("id_usuario"),
            quiz_type = c.getStringOrNull("quiz_type"),
            week_id = c.getIntOrNull("week_id"),
            session_id = c.getIntOrNull("session_id"),
            quiz_title = c.getStringOrNull("quiz_title"),
            fecha_registro = c.getStringOrNull("fecha_registro"),
            fecha_sincronizacion = c.getStringOrNull("fecha_sincronizacion"),
            total_questions = c.getIntOrNull("total_questions"),
            correct_answers = c.getIntOrNull("correct_answers"),
            percentage_correct = c.getDoubleOrNull("percentage_correct"),
            multimedia_unique_key = c.getStringOrNull("multimedia_unique_key"),
            treatment_type = c.getStringOrNull("treatment_type")
        )
    }

    /**
     * Return all question responses for a given quiz response ID.
     */
    fun getQuestionResponsesForQuiz(quizResponseId: Long): List<QuestionResponseRecord> {
        val results = mutableListOf<QuestionResponseRecord>()
        val db = readableDatabase

        val selection = "id_quiz_response = ?"
        val selectionArgs = arrayOf(quizResponseId.toString())

        val cursor = db.query(
            "question_response_table",
            null, // All columns
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    results.add(cursorToQuestionResponseRecord(c))
                } while (c.moveToNext())
            }
        }
        return results
    }

    private fun cursorToQuestionResponseRecord(c: Cursor): QuestionResponseRecord {
        return QuestionResponseRecord(
            id_question_response = c.getLongOrNull("id_question_response") ?: -1L,
            id_quiz_response = c.getLongOrNull("id_quiz_response"),
            question_index = c.getIntOrNull("question_index"),
            question_text = c.getStringOrNull("question_text"),
            user_answer_index = c.getIntOrNull("user_answer_index"),
            user_answer_text = c.getStringOrNull("user_answer_text"),
            correct_answer_index = c.getIntOrNull("correct_answer_index"),
            correct_answer_text = c.getStringOrNull("correct_answer_text"),
            is_correct = c.getIntOrNull("is_correct"),
            posibles_respuestas = c.getStringOrNull("posibles_respuestas")
        )
    }
        
            fun upsertPais(pais: Pais): Long {
                val db = writableDatabase
                val values = ContentValues().apply {
                    put("id", pais.id)
                    put("nombre", pais.nombre)
                }
                return db.insertWithOnConflict("paises", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        
            fun upsertCentroSalud(centroSalud: CentroSalud): Long {
                val db = writableDatabase
                val values = ContentValues().apply {
                    put("id", centroSalud.id)
                    put("nombre", centroSalud.nombre)
                }
                return db.insertWithOnConflict("centros_salud", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        
            fun upsertUsuario(usuario: Usuario) {
                upsertPais(usuario.pais)
                upsertCentroSalud(usuario.centroSalud)
        
                val db = writableDatabase
                val values = ContentValues().apply {
                    put("idUsuario", usuario.id)
                    put("nombre", usuario.nombre)
                    put("documento", usuario.documento)
                    put("email", usuario.email)
                    put("sexo", usuario.sexo)
                    put("id_pais", usuario.pais.id)
                    put("id_centro_salud", usuario.centroSalud.id)
                    put("primer_login", usuario.primerLogin)
                }
                db.insertWithOnConflict("usuarios", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        
            fun getUsuarioPrimerLogin(idUsuario: Int): String? {
                return try {
                    val db = readableDatabase
                    val query = "SELECT primer_login FROM usuarios WHERE idUsuario = ?"
                    db.rawQuery(query, arrayOf(idUsuario.toString())).use { c ->
                        if (c.moveToFirst()) {
                            c.getStringOrNull("primer_login")
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            fun debugSubtractWeekFromRegistrationDate(idUsuario: Int) {
                val db = writableDatabase
                val query = "SELECT primer_login FROM usuarios WHERE idUsuario = ?"
                var currentFecha: String? = null
                db.rawQuery(query, arrayOf(idUsuario.toString())).use { c ->
                    if (c.moveToFirst()) {
                        currentFecha = c.getStringOrNull("primer_login")
                    }
                }

                if (currentFecha != null) {
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    try {
                        val date = formatter.parse(currentFecha)
                        if (date != null) {
                            // Subtract 7 days (7 * 24 * 60 * 60 * 1000)
                            val newDate = Date(date.time - 604800000L)
                            val newFechaString = formatter.format(newDate)
                            val cv = ContentValues().apply {
                                put("primer_login", newFechaString)
                            }
                            db.update("usuarios", cv, "idUsuario = ?", arrayOf(idUsuario.toString()))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            fun getUsuario(id: Int): Usuario? {
                val db = readableDatabase
                val query = """
                    SELECT
                        u.idUsuario, u.documento, u.nombre, u.email, u.sexo,
                        p.id as id_pais, p.nombre as nombre_pais,
                        cs.id as id_centro_salud, cs.nombre as nombre_centro_salud
                    FROM usuarios u
                    LEFT JOIN paises p ON u.id_pais = p.id
                    LEFT JOIN centros_salud cs ON u.id_centro_salud = cs.id
                    WHERE u.idUsuario = ?
                """.trimIndent()

                db.rawQuery(query, arrayOf(id.toString())).use { c ->
                    if (c.moveToFirst()) {
                        val paisId = c.getIntOrNull("id_pais")
                        val paisNombre = c.getStringOrNull("nombre_pais")
                        val centroId = c.getIntOrNull("id_centro_salud")
                        val centroNombre = c.getStringOrNull("nombre_centro_salud")

                        if (paisId == null || paisNombre == null || centroId == null || centroNombre == null) {
                            return null
                        }

                        val pais = Pais(id = paisId, nombre = paisNombre)
                        val centroSalud = CentroSalud(id = centroId, nombre = centroNombre)

                        return Usuario(
                            id = c.getIntOrNull("idUsuario") ?: return null,
                            documento = c.getStringOrNull("documento") ?: "",
                            nombre = c.getStringOrNull("nombre") ?: "",
                            email = c.getStringOrNull("email") ?: "",
                            sexo = c.getStringOrNull("sexo") ?: "",
                            pais = pais,
                            centroSalud = centroSalud
                        )
                    }
                }
                return null
            }

            /**
             * Get the user's country ID (id_pais)
             * Returns null if user not found or country not set
             */
            fun getUsuarioPaisId(idUsuario: Int): Int? {
                val db = readableDatabase
                val query = "SELECT id_pais FROM usuarios WHERE idUsuario = ?"
                db.rawQuery(query, arrayOf(idUsuario.toString())).use { c ->
                    if (c.moveToFirst()) {
                        return c.getIntOrNull("id_pais")
                    }
                }
                return null
            }

            /**
             * Check if the user has recorded high blood pressure (>= 140/90) for 4 consecutive days.
             */
            fun hasHighPressureTrend(idUsuario: Int): Boolean {
                val db = readableDatabase
                // Get the latest 4 unique days with high pressure readings
                val query = """
                    SELECT DISTINCT date(fecha_hora_edicion) as day_date 
                    FROM presion_table 
                    WHERE id_usuario = ? 
                    AND (sistolica >= 140 OR diastolica >= 90) 
                    ORDER BY day_date DESC 
                    LIMIT 4
                """.trimIndent()
                
                val dates = mutableListOf<String>()
                db.rawQuery(query, arrayOf(idUsuario.toString())).use { cursor ->
                    while (cursor.moveToNext()) {
                        dates.add(cursor.getString(0))
                    }
                }
                
                if (dates.size < 4) return false
                
                // Check if they are consecutive
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                return try {
                    for (i in 0 until dates.size - 1) {
                        val d1 = sdf.parse(dates[i]) ?: return false
                        val d2 = sdf.parse(dates[i+1]) ?: return false
                        val diff = (d1.time - d2.time) / (1000 * 60 * 60 * 24)
                        if (diff != 1L) return false
                    }
                    true
                } catch (e: Exception) {
                    false
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