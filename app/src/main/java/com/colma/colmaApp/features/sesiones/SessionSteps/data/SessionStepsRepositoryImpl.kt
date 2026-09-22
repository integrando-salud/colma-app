package com.colma.colmaApp.features.sesiones.SessionSteps.data

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.colma.colmaApp.R
import com.colma.colmaApp.features.content.domain.ContentRepository
import com.colma.colmaApp.features.content.domain.SubSessionProgress
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaKeyUtils
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.SessionStepsRepository
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionStepsRepositoryImpl(
    private val contentRepository: ContentRepository,
    private val context: Context,
    private val dbHelper: DbHelper
) : SessionStepsRepository {

    private val TAG = "SessionStepsRepo"
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
    ): Result<List<MultimediaOption>> = withContext(Dispatchers.IO) {
        // Assuming we only care about the first treatment type for now based on previous logic
        val treatmentType = treatmentTypes.firstOrNull() ?: return@withContext Result.failure(Exception("No treatment type provided"))

        contentRepository.getMultimediaOptions(treatmentType, sessionId, subSessionId).fold(
            onSuccess = { multimediaContents ->
                val processedContents = if (treatmentType == TreatmentType.DBT_HTA) {
                    val hasMetas = multimediaContents.any { it.type == MultimediaContentType.METAS }
                    if (hasMetas) {
                        val metasTemplate = multimediaContents.first { it.type == MultimediaContentType.METAS }
                        val unifiedMetas = metasTemplate.copy(
                            title = "Metas (Diabetes e Hipertensión)",
                            metasId = "dbt_hta"
                        )
                        val resultList = mutableListOf<com.colma.colmaApp.features.content.model.MultimediaContent>()
                        var addedUnified = false
                        for (item in multimediaContents) {
                            if (item.type == MultimediaContentType.METAS) {
                                if (!addedUnified) {
                                    resultList.add(unifiedMetas)
                                    addedUnified = true
                                }
                            } else {
                                resultList.add(item)
                            }
                        }
                        resultList
                    } else {
                        multimediaContents
                    }
                } else {
                    multimediaContents
                }

                val size = processedContents.size
                Result.success(processedContents.mapIndexed { index, multimediaContent ->
                    val isLocalFile = multimediaContent.contentAsset?.contains("/") == true || multimediaContent.contentAsset?.contains("\\") == true
                    val localFilePath = if (isLocalFile) multimediaContent.contentAsset else null
                    
                    val contentAssetId = if (isLocalFile) null else multimediaContent.contentAsset?.let {
                        val resId = ResourceUtils.getResourceId(context, it, "raw")
                        if (resId != 0) resId else null
                    }
                    
                    val originalTitle = multimediaContent.title.trim()
                    val finalTitle = if (originalTitle.isNotEmpty()) {
                        originalTitle
                    } else if (multimediaContent.type == MultimediaContentType.DESCRIPTION) {
                        if (index == 0) {
                            "Bienvenida"
                        } else if (index == size - 1 && size > 1) {
                            "Fin de la sesión"
                        } else {
                            "Descripción"
                        }
                    } else {
                        multimediaContent.type.name
                    }

                    val uniqueKey = generateMultimediaUniqueKey(
                        multimediaContent.type,
                        finalTitle,
                        contentAssetId,
                        multimediaContent.metasId,
                        treatmentType.name,
                        sessionId,
                        subSessionId,
                        localFilePath
                    )
                    
                    val isCompleted = dbHelper.getMultimediaReadRecordExists(
                        idUsuario = userId,
                        treatmentId = treatmentType.name,
                        weekId = sessionId.toInt(),
                        sessionId = subSessionId.toInt(),
                        multimediaType = multimediaContent.type.name,
                        multimediaUniqueKey = uniqueKey
                    )

                    val localDescriptionAudios = multimediaContent.descriptionAudios?.filter { it.contains("/") || it.contains("\\") }
                    val staticDescriptionAudioRes = multimediaContent.descriptionAudios?.filter { !it.contains("/") && !it.contains("\\") }?.map { audioName ->
                        ResourceUtils.getResourceId(context, audioName, "raw")
                    }

                    val originalDescription = multimediaContent.description
                    val originalFullDescription = multimediaContent.fullDescription

                    val finalDescription = if (multimediaContent.type == MultimediaContentType.DESCRIPTION) {
                        var cleaned = originalDescription.replace("(?i)\\s*Semana\\s*\\d+:\\s*.*$".toRegex(), "")
                        if (index == 0) {
                            cleaned = cleaned.replace("(?i)^.*?(?=(¡Hola|¡Felicitaciones))".toRegex(), "")
                        } else if (index == size - 1 && size > 1) {
                            cleaned = cleaned.replace("(?i)^(?:Bienvenida/Cierre:\\s*Texto\\s*(?:Fin de (la )?sesión)?|Fin de (la )?sesión)\\s*".toRegex(), "")
                        }
                        cleaned
                    } else {
                        originalDescription
                    }

                    val finalFullDescription = if (multimediaContent.type == MultimediaContentType.DESCRIPTION) {
                        var cleaned = originalFullDescription?.replace("(?i)\\s*Semana\\s*\\d+:\\s*.*$".toRegex(), "")
                        if (index == 0) {
                            cleaned = cleaned?.replace("(?i)^.*?(?=(¡Hola|¡Felicitaciones))".toRegex(), "")
                        } else if (index == size - 1 && size > 1) {
                            cleaned = cleaned?.replace("(?i)^(?:Bienvenida/Cierre:\\s*Texto\\s*(?:Fin de (la )?sesión)?|Fin de (la )?sesión)\\s*".toRegex(), "")
                        }
                        cleaned
                    } else {
                        originalFullDescription
                    }

                    MultimediaOption(
                        type = multimediaContent.type,
                        title = finalTitle,
                        description = finalDescription,
                        fullDescription = finalFullDescription,
                        iconResId = multimediaContent.iconResId?.let {
                            ResourceUtils.getResourceId(context, it, "drawable")
                        },
                        iconImageVector = multimediaContent.iconImageVector?.let {
                            mapImageVector(it)
                        },
                        contentAssetId = contentAssetId,
                        questions = multimediaContent.questions,
                        metasId = multimediaContent.metasId,
                        metasOptions = multimediaContent.metasOptions,
                        descriptionAudios = staticDescriptionAudioRes,
                        isCompleted = isCompleted,
                        localFilePath = localFilePath,
                        localDescriptionAudios = localDescriptionAudios,
                        localDescriptionImages = multimediaContent.descriptionImages,
                        tratamientoMultimediaId = multimediaContent.tratamientoMultimediaId
                    )
                })
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun generateMultimediaUniqueKey(
        type: MultimediaContentType,
        title: String,
        contentAssetId: Int?,
        metasId: String?,
        treatmentId: String,
        weekId: Long,
        sessionId: Long,
        localFilePath: String? = null
    ): String {
        return MultimediaKeyUtils.generateMultimediaUniqueKey(
            type = type,
            title = title,
            contentAssetId = contentAssetId,
            metasId = metasId,
            treatmentId = treatmentId,
            weekId = weekId,
            sessionId = sessionId,
            localFilePath = localFilePath
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
        val treatmentId = multimediaReadRecord.treatment_id ?: "UNKNOWN"
        val weekId = multimediaReadRecord.week_id ?: -1
        val sessionId = multimediaReadRecord.session_id ?: -1

        dbHelper.upsertMultimediaRead(
            idUsuario = userId,
            treatmentId = treatmentId,
            weekId = weekId,
            sessionId = sessionId,
            multimediaType = multimediaReadRecord.multimedia_type ?: "UNKNOWN",
            multimediaUniqueKey = multimediaReadRecord.multimedia_unique_key ?: "UNKNOWN",
            tratamientoMultimediaId = multimediaReadRecord.tratamiento_multimedia_id
        )

        // Check if the subsession has been completed
        val treatmentType = try {
            TreatmentType.valueOf(treatmentId.uppercase())
        } catch (e: Exception) {
            null
        }

        if (treatmentType != null && weekId > 0 && sessionId > 0) {
            val optionsResult = getMultimediaOptions(listOf(treatmentType), weekId.toLong(), sessionId.toLong())
            optionsResult.onSuccess { options ->
                if (options.isNotEmpty() && options.all { it.isCompleted }) {
                    dbHelper.insertCompletedSession(
                        idUsuario = userId,
                        treatmentId = treatmentId,
                        weekId = weekId,
                        sessionId = sessionId
                    )
                }
            }
        }
    }

    override suspend fun getSubSessionProgress(
        treatmentTypes: List<TreatmentType>,
        sessionId: Long,
        subSessionId: Long
    ): Result<SubSessionProgress> = withContext(Dispatchers.IO) {
        val treatmentType = treatmentTypes.firstOrNull() ?: return@withContext Result.failure(Exception("No treatment type provided"))
        contentRepository.getSubSessionProgress(treatmentType, sessionId, subSessionId)
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
                tratamientoElementoId = quizResponse.tratamiento_elemento_id,
                idQuiz = quizResponse.id_quiz_response.toInt(),
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


            val completedSessionsCount = dbHelper.getCompletedSessionsCount(userId)
            val syncRequest = SyncDataRequest(
                deviceTimestamp = System.currentTimeMillis(),
                userId = userId,
                app = "colmaApp",
                clientSyncTimestamp = now,
                sesionesCompletadas = completedSessionsCount,
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
