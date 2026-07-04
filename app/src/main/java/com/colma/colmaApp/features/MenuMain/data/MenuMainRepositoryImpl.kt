package com.colma.colmaApp.features.MenuMain.data

import com.colma.colmaApp.features.MenuMain.domain.MenuMainRepository
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.internetCommunication.SyncService
import com.colma.colmaApp.servicios.internetCommunication.HelpRequestService
import com.colma.colmaApp.servicios.session.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.content.Context
import com.colma.colmaApp.utils.NetworkUtils
import java.io.IOException

class MenuMainRepositoryImpl(
    private val context: Context,
    private val dbHelper: DbHelper
) : MenuMainRepository {

    private val TAG = "MenuMainRepositoryImpl"
    private val syncService = SyncService()
    private val helpRequestService = HelpRequestService()

    override suspend fun getPendingPastMeta(): RegistrosSaludModelos.MetaRecord? {
        val userId = SessionManager.getUserId()
        return withContext(Dispatchers.IO) {
            dbHelper.getPendingPastMeta(userId)
        }
    }

    override suspend fun getPendingFutureMetasCount(): Int {
        val userId = SessionManager.getUserId()
        return withContext(Dispatchers.IO) {
            dbHelper.getPendingFutureMetasCount(userId)
        }
    }

    override suspend fun updateMeta(meta: RegistrosSaludModelos.MetaRecord) {
        withContext(Dispatchers.IO) {
            dbHelper.updateMeta(meta)
        }
    }

    suspend fun createSyncDataPayload(userId: Int): com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest {
        val clientSyncTimestamp = nowLocal()

        // Fetch all pending data in parallel
        val pendingGlucosa = withContext(Dispatchers.IO) { dbHelper.getPendingGlucosa(userId) }
        val pendingPresion = withContext(Dispatchers.IO) { dbHelper.getPendingPresion(userId) }
        val pendingEstadoAnimo = withContext(Dispatchers.IO) { dbHelper.getPendingEstadoAnimoList(userId) }
        val pendingMetas = withContext(Dispatchers.IO) { dbHelper.getPendingMetas(userId) }
        val pendingMultimediaRead = withContext(Dispatchers.IO) { dbHelper.getPendingMultimediaRead(userId) }
        val pendingQuizzes = withContext(Dispatchers.IO) { dbHelper.getPendingQuizResponses(userId) }

        // Map to DTOs
        val glucosaData = pendingGlucosa.map { g ->
            com.colma.colmaApp.servicios.internetCommunication.model.GlucosaData(
                idGlucosa = g.id.toInt(),
                idUsuario = g.idUsuario ?: userId,
                valorMgdl = g.valorMgdl?.toFloat() ?: 0f,
                ayuno = g.ayuno ?: 0,
                fechaRegistro = g.fecha_registro ?: clientSyncTimestamp,
                fechaUsuario = g.fecha_hora_edicion ?: g.fecha_registro ?: clientSyncTimestamp,
                fechaSincronizacion = clientSyncTimestamp,
                advertencia = g.advertencia ?: ""
            )
        }

        val presionData = pendingPresion.map { p ->
            com.colma.colmaApp.servicios.internetCommunication.model.PresionData(
                idPresion = p.id.toInt(),
                idUsuario = p.idUsuario ?: userId,
                sistolica = p.sistolica?.toFloat() ?: 0f,
                diastolica = p.diastolica?.toFloat() ?: 0f,
                fechaRegistro = p.fecha_registro ?: clientSyncTimestamp,
                fechaUsuario = p.fecha_hora_edicion ?: p.fecha_registro ?: clientSyncTimestamp,
                fechaSincronizacion = clientSyncTimestamp,
                advertencia = p.advertencia ?: ""
            )
        }

        val estadoAnimoData = pendingEstadoAnimo.map { e ->
            com.colma.colmaApp.servicios.internetCommunication.model.EstadoAnimoData(
                idEstadoAnimo = e.rowid.toInt(),
                idUsuario = e.idPaciente ?: userId,
                fechaRegistro = e.fechaRegistro ?: clientSyncTimestamp,
                fechaUsuario = e.fechaEdicion ?: e.fechaRegistro ?: clientSyncTimestamp,
                pregunta1 = e.preguntas.getOrNull(0) ?: 0,
                pregunta2 = e.preguntas.getOrNull(1) ?: 0,
                pregunta3 = e.preguntas.getOrNull(2) ?: 0,
                pregunta4 = e.preguntas.getOrNull(3) ?: 0,
                pregunta5 = e.preguntas.getOrNull(4) ?: 0,
                pregunta6 = e.preguntas.getOrNull(5) ?: 0,
                pregunta7 = e.preguntas.getOrNull(6) ?: 0,
                pregunta8 = e.preguntas.getOrNull(7) ?: 0,
                pregunta9 = e.preguntas.getOrNull(8) ?: 0,
                fechaSincronizacion = clientSyncTimestamp
            )
        }

        val quizData = pendingQuizzes.map { q ->
            val preguntas = dbHelper.getQuestionResponsesForQuiz(q.id_quiz_response)
            val preguntaData = preguntas.map { p ->
                val posiblesRespuestas = try {
                    Gson().fromJson(p.posibles_respuestas, Array<String>::class.java).toList()
                } catch (e: Exception) {
                    emptyList()
                }
                com.colma.colmaApp.servicios.internetCommunication.model.PreguntaData(
                    preguntaIndex = p.question_index ?: 0,
                    preguntaTexto = p.question_text ?: "",
                    respuestaUsuario = p.user_answer_index ?: -1,
                    respuestaUsuarioTexto = p.user_answer_text ?: "",
                    respuestaCorrecta = p.correct_answer_index ?: -1,
                    respuestaCorrectaTexto = p.correct_answer_text ?: "",
                    esCorrecta = p.is_correct == 1,
                    posiblesRespuestas = posiblesRespuestas
                )
            }
            com.colma.colmaApp.servicios.internetCommunication.model.QuizData(
                idQuiz = q.id_quiz_response.toInt(),
                idUsuario = q.id_usuario ?: userId,
                quizType = (q.quiz_type ?: "").lowercase(Locale.ROOT),
                sessionId = q.session_id ?: 0,
                subSessionId = null, // Not available in local DB model
                fechaRegistro = q.fecha_registro ?: clientSyncTimestamp,
                fechaUsuario = q.fecha_registro ?: clientSyncTimestamp, // No separate user date
                fechaSincronizacion = clientSyncTimestamp,
                preguntas = preguntaData,
                puntajeTotal = q.total_questions ?: 0,
                puntajeObtenido = q.correct_answers ?: 0,
                porcentajeCorrecto = q.percentage_correct?.toInt() ?: 0
            )
        }

        val metaData = pendingMetas
            .filter { it.meta_cumplida != null && it.meta_cumplida != 0 }
            .map { m ->
                com.colma.colmaApp.servicios.internetCommunication.model.MetaData(
                    idMeta = m.id_meta.toInt(),
                    idUsuario = m.id_usuario ?: userId,
                    semana = m.semana ?: 0,
                    sesion = m.sesion ?: 0,
                    tipoActividad = m.tipo_actividad ?: "",
                    actividad = m.actividad ?: "",
                    lugar = m.lugar ?: "",
                    acompaniante = m.acompañante,
                    fechaRegistro = m.fecha_regisitro ?: clientSyncTimestamp,
                    fechaUsuario = m.fecha_edicion ?: m.fecha_regisitro ?: clientSyncTimestamp,
                    fechaEdicion = m.fecha_edicion,
                    fechaSincronizacion = clientSyncTimestamp,
                    metaCumplida = (m.meta_cumplida == 2),
                    fechaObjetivo = m.fecha_objetivo ?: clientSyncTimestamp,
                    motivoIncumplimiento = m.motivo_incumplimiento
                )
            }

        val multimediaReadData = pendingMultimediaRead.map { mr ->
            com.colma.colmaApp.servicios.internetCommunication.model.MultimediaReadData(
                idMultimediaRead = mr.id_multimedia_read.toInt(),
                idUsuario = mr.id_usuario ?: userId,
                treatmentId = mr.treatment_id ?: "",
                weekId = mr.week_id ?: 0,
                sessionId = mr.session_id ?: 0,
                multimediaType = mr.multimedia_type ?: "",
                multimediaUniqueKey = mr.multimedia_unique_key ?: "",
                fechaRegistro = mr.fecha_registro ?: clientSyncTimestamp,
                fechaSincronizacion = clientSyncTimestamp
            )
        }

        return com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest(
            deviceTimestamp = System.currentTimeMillis(),
            userId = userId,
            app = "colmaApp",
            clientSyncTimestamp = clientSyncTimestamp,
            glucosa = glucosaData,
            presion = presionData,
            estadoAnimo = estadoAnimoData,
            quiz = quizData,
            metas = metaData,
            multimediaRead = multimediaReadData
        )
    }

    override suspend fun synchronizeData(): Result<String> {
        val userId = SessionManager.getUserId() ?: return Result.failure(Exception("User not logged in"))
        val token = SessionManager.getAuthToken() ?: return Result.failure(Exception("User not authenticated"))

        if (!NetworkUtils.isInternetAvailable(context)) {
            return Result.failure(IOException("No internet connection"))
        }

        return try {
            val syncRequest = createSyncDataPayload(userId)

            // Log the formatted JSON payload (always logged, even if empty)
            val requestBodyJson = GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(syncRequest)
            Log.d(TAG, "Sync Request Payload for user $userId:\n$requestBodyJson")

            // Exit early if there is nothing to sync
            if (syncRequest.glucosa.isEmpty() && syncRequest.presion.isEmpty() && syncRequest.estadoAnimo.isEmpty() && syncRequest.quiz.isEmpty() && syncRequest.metas.isEmpty() && syncRequest.multimediaRead.isEmpty()) {
                Log.d(TAG, "No data to synchronize for user $userId.")
                return Result.success("No data to synchronize.")
            }

            val response = syncService.syncData(token, syncRequest) // Removed "Bearer " prefix as it's handled in SyncService

            if (response.isSuccessful) {
                // Log the success response body
                val responseBody = response.body()?.message ?: "Synchronization successful"
                Log.d(TAG, "Sync Response for user $userId: $responseBody")

                // Mark all sent records as synchronized in the local DB
                dbHelper.markGlucosaSent(syncRequest.glucosa.map { it.idGlucosa.toLong() })
                dbHelper.markPresionSent(syncRequest.presion.map { it.idPresion.toLong() })
                dbHelper.updateEstadoAnimoSyncTimestamps(syncRequest.estadoAnimo.associate { it.idEstadoAnimo.toLong() to nowLocal() })
                dbHelper.markQuizResponsesSent(syncRequest.quiz.map { it.idQuiz.toLong() })
                dbHelper.markMetasSent(syncRequest.metas.map { it.idMeta.toLong() })
                dbHelper.markMultimediaReadSent(syncRequest.multimediaRead.map { it.idMultimediaRead.toLong() })

                dbHelper.insertSincronizacion(nowLocal(), "200", null)

                Result.success(response.body()?.message ?: "Synchronization successful")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val jsonRequest = Gson().toJson(syncRequest)
                Log.e(TAG, "Sync Error Response for user $userId: ${response.code()} - $errorBody")
                dbHelper.insertSincronizacion(nowLocal(), "HTTP ${response.code()}", jsonRequest)
                Result.failure(Exception("Synchronization failed: $errorBody"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun saveSynchronizationRecord(timestamp: String, status: String, data: String?) {
        withContext(Dispatchers.IO) {
            dbHelper.insertSincronizacion(timestamp, status, data)
        }
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            SessionManager.clearSession()
        }
    }

    override suspend fun getUserName(): String? {
        val userId = SessionManager.getUserId()
        return withContext(Dispatchers.IO) {
            userId?.let { dbHelper.getUsuario(it)?.nombre }
        }
    }

    override suspend fun requestHelp(): Result<String> {
        val token = SessionManager.getAuthToken() ?: return Result.failure(Exception("User not authenticated"))

        return withContext(Dispatchers.IO) {
            try {
                val response = helpRequestService.requestHelp(token)
                if (response.isSuccessful) {
                    Result.success(response.body()?.message ?: "Pedido de ayuda registrado con éxito.")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Help Request Error: ${response.code()} - $errorBody")
                    Result.failure(Exception("Error al registrar el pedido de ayuda: $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during requestHelp: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun getDownloadedTreatmentTypes(): List<String> = withContext(Dispatchers.IO) {
        val contentRepo = com.colma.colmaApp.features.content.data.ContentRepositoryImpl(context)
        contentRepo.getAllTreatments().fold(
            onSuccess = { treatments ->
                treatments.map { it.id.name }
            },
            onFailure = {
                listOf("HTA", "DIABETES")
            }
        )
    }

    private fun nowLocal(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(Date())
    }
}
