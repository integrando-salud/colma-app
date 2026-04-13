package com.colma.colmaApp.features.sesiones.MultimediaSelector.data

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.colma.colmaApp.R
import com.colma.colmaApp.features.content.domain.ContentRepository
import com.colma.colmaApp.features.content.domain.SubSessionProgress
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaRepository
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.utils.NetworkUtils
import com.colma.colmaApp.utils.ResourceUtils
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord
import com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.internetCommunication.SyncService
import com.colma.colmaApp.servicios.internetCommunication.model.PreguntaData
import com.colma.colmaApp.servicios.internetCommunication.model.QuizData
import com.colma.colmaApp.servicios.internetCommunication.model.SyncDataRequest
import com.colma.colmaApp.servicios.session.SessionManager
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MultimediaRepositoryImpl(
    private val contentRepository: ContentRepository,
    private val context: Context,
    private val dbHelper: DbHelper
) : MultimediaRepository {

    private val TAG = "MultimediaRepo"
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val syncService = SyncService()
    private val userId: Int

    init {
        SessionManager.init(context.applicationContext)
        userId = SessionManager.getUserId() ?: 1 // Default to 1 if not found
    }

    override suspend fun getMultimediaOptions(
        treatmentTypes: List<TreatmentType>,
        sessionId: Long,
        subSessionId: Long
    ): Result<List<MultimediaOption>> {
        // Assuming we only care about the first treatment type for now based on previous logic
        val treatmentType = treatmentTypes.firstOrNull() ?: return Result.failure(Exception("No treatment type provided"))

        return contentRepository.getMultimediaOptions(treatmentType, sessionId, subSessionId).fold(
            onSuccess = { multimediaContents ->
                Result.success(multimediaContents.map { multimediaContent ->
                    MultimediaOption(
                        type = multimediaContent.type,
                        title = multimediaContent.title,
                        description = multimediaContent.description,
                        fullDescription = multimediaContent.fullDescription,
                        iconResId = multimediaContent.iconResId?.let {
                            ResourceUtils.getResourceId(context, it, "drawable")
                        },
                        iconImageVector = multimediaContent.iconImageVector?.let {
                            mapImageVector(it)
                        },
                        contentAssetId = multimediaContent.contentAsset?.let {
                            ResourceUtils.getResourceId(context, it, "raw")
                        },
                        questions = multimediaContent.questions,
                        metasId = multimediaContent.metasId
                    )
                })
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun mapImageVector(name: String): ImageVector? {
        // This is a simple mapping. More complex solutions might use reflection or a larger map.
        return when (name) {
            "PlayArrow" -> Icons.Default.PlayArrow
            // Add other ImageVector mappings as needed
            else -> null
        }
    }

    override suspend fun saveMultimediaRead(multimediaReadRecord: RegistrosSaludModelos.MultimediaReadRecord) {
        dbHelper.upsertMultimediaRead(
            idUsuario = userId,
            treatmentId = multimediaReadRecord.treatment_id ?: "UNKNOWN",
            weekId = multimediaReadRecord.week_id ?: -1,
            sessionId = multimediaReadRecord.session_id ?: -1,
            multimediaType = multimediaReadRecord.multimedia_type ?: "UNKNOWN",
            multimediaUniqueKey = multimediaReadRecord.multimedia_unique_key ?: "UNKNOWN"
        )
    }

    override suspend fun getSubSessionProgress(
        treatmentTypes: List<TreatmentType>,
        sessionId: Long,
        subSessionId: Long
    ): Result<SubSessionProgress> {
        val treatmentType = treatmentTypes.firstOrNull() ?: return Result.failure(Exception("No treatment type provided"))
        return contentRepository.getSubSessionProgress(treatmentType, sessionId, subSessionId)
    }

    override suspend fun saveQuizResponses(quizResponse: QuizResponseRecord, questionResponses: List<QuestionResponseRecord>): Result<Long> {
        return try {
            val updatedQuizResponse = quizResponse.copy(id_usuario = userId)
            val quizResponseId = dbHelper.insertQuizResponse(updatedQuizResponse, questionResponses)
            if (quizResponseId > 0L) {
                Result.success(quizResponseId)
            } else {
                Result.failure(Exception("Error guardando respuestas del quiz en la base de datos"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun syncQuizResponse(quizResponse: QuizResponseRecord, questionResponses: List<QuestionResponseRecord>): Result<String> {
        if (!NetworkUtils.isInternetAvailable(context)) {
            Log.w(TAG, "Sync skipped: No internet connection.")
            return Result.failure(Exception("No hay conexión a internet"))
        }

        val token = SessionManager.getAuthToken() ?: run {
            Log.e(TAG, "Sync failed: No auth token found.")
            return Result.failure(Exception("No autenticado"))
        }
        val userId = SessionManager.getUserId() ?: run {
            Log.e(TAG, "Sync failed: No user ID found.")
            return Result.failure(Exception("Usuario no identificado"))
        }

        return try {
            val now = isoFormatter.format(Date())
            
            // Convert question responses to PreguntaData
            val preguntasData = questionResponses.map { q ->
                val posiblesRespuestas = try {
                    Gson().fromJson(q.posibles_respuestas, Array<String>::class.java).toList()
                } catch (e: Exception) {
                    emptyList()
                }
                
                PreguntaData(
                    preguntaIndex = q.question_index ?: 0,
                    preguntaTexto = q.question_text ?: "",
                    respuestaUsuario = q.user_answer_index ?: -1,
                    respuestaUsuarioTexto = q.user_answer_text ?: "",
                    respuestaCorrecta = q.correct_answer_index ?: -1,
                    respuestaCorrectaTexto = q.correct_answer_text ?: "",
                    esCorrecta = q.is_correct == 1,
                    posiblesRespuestas = posiblesRespuestas
                )
            }

            val quizType = (quizResponse.quiz_type ?: "unknown").lowercase(Locale.getDefault())
            
            val quizData = QuizData(
                idQuiz = quizResponse.id_quiz_response?.toInt() ?: 0,
                idUsuario = userId,
                quizType = quizType,
                sessionId = quizResponse.session_id ?: 0,
                subSessionId = null,
                fechaRegistro = quizResponse.fecha_registro ?: now,
                fechaUsuario = quizResponse.fecha_registro ?: now,
                fechaSincronizacion = now,
                preguntas = preguntasData,
                puntajeTotal = quizResponse.total_questions ?: 0,
                puntajeObtenido = quizResponse.correct_answers ?: 0,
                porcentajeCorrecto = quizResponse.percentage_correct?.toInt() ?: 0
            )

            val syncRequest = SyncDataRequest(
                deviceTimestamp = System.currentTimeMillis(),
                userId = userId,
                app = "colmaApp",
                clientSyncTimestamp = now,
                glucosa = emptyList(),
                presion = emptyList(),
                estadoAnimo = emptyList(),
                quiz = listOf(quizData),
                metas = emptyList(),
                multimediaRead = emptyList()
            )

            val requestBodyJson = Gson().toJson(syncRequest)
            Log.d(TAG, "Sync Request Body: $requestBodyJson")

            Log.d(TAG, "Attempting to sync quiz entry: ${quizResponse.id_quiz_response}")
            val response = syncService.syncData(token, syncRequest)
            if (response.isSuccessful) {
                Log.d(TAG, "Sync successful for quiz entry ${quizResponse.id_quiz_response}: ${response.body()?.message}")
                Result.success(response.body()?.message ?: "Sincronización exitosa")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Sync failed for quiz entry ${quizResponse.id_quiz_response}. Code: ${response.code()}, Error: $errorMsg")
                Result.failure(Exception("Error en sincronización: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sync for quiz entry ${quizResponse.id_quiz_response}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
