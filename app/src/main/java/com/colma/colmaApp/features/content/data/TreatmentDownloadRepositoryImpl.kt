package com.colma.colmaApp.features.content.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.colma.colmaApp.features.content.domain.TreatmentDownloadRepository
import com.colma.colmaApp.features.content.model.ContentDictionary
import com.colma.colmaApp.features.content.model.MultimediaContent
import com.colma.colmaApp.features.content.model.QuizQuestion
import com.colma.colmaApp.features.content.model.SessionContent
import com.colma.colmaApp.features.content.model.SubSessionContent
import com.colma.colmaApp.features.content.model.TreatmentContent
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.servicios.internetCommunication.TreatmentService
import com.colma.colmaApp.servicios.internetCommunication.model.*
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.google.gson.Gson

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaKeyUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TreatmentDownloadRepositoryImpl(private val context: Context) : TreatmentDownloadRepository {

    private val TAG = "TreatmentDownloadRepo"
    private val treatmentService = TreatmentService()

    override suspend fun downloadAndSaveTreatment(
        token: String,
        userId: Int,
        onProgress: ((progress: Float, status: String) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val mockAssetFile = when {
                token == "mock_test_token_ar_dbt_hta" || userId == 9999 -> "mock_treatment_ar_dbt_hta.json"
                token == "mock_test_token_ar_dbt" || userId == 9998 -> "mock_treatment_ar_dbt.json"
                token == "mock_test_token_ar_hta" || userId == 9997 -> "mock_treatment_ar_hta.json"
                token == "mock_test_token_pe_dbt_hta" || userId == 9996 -> "mock_treatment_pe_dbt_hta.json"
                token == "mock_test_token_pe_dbt" || userId == 9995 -> "mock_treatment_pe_dbt.json"
                token == "mock_test_token_pe_hta" || userId == 9994 -> "mock_treatment_pe_hta.json"
                else -> null
            }
            val isMockTestUser = mockAssetFile != null
            onProgress?.invoke(0.05f, "Conectando al servidor...")
            Log.d(TAG, "Fetching treatment from server (isMockTestUser=$isMockTestUser, mockAssetFile=$mockAssetFile)...")

            val bodyString = if (mockAssetFile != null) {
                Log.d(TAG, "Loading mock treatment from assets/$mockAssetFile")
                context.assets.open(mockAssetFile).bufferedReader().use { it.readText() }
            } else {
                val response = treatmentService.getTratamiento(token)
                if (!response.isSuccessful) {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Failed to fetch treatment: code=${response.code()}, error=$errorMsg")
                    return@withContext Result.failure(IOException("Failed to fetch treatment: ${response.code()}"))
                }
                response.body()?.string() ?: ""
            }
            Log.d(TAG, "Successfully fetched treatment JSON response.")
            Log.d(TAG, "Server Treatment Response Body: $bodyString")

            val serverResponse = try {
                Gson().fromJson(bodyString, ServerTreatmentResponse::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "JSON parsing error: ${e.message}")
                return@withContext Result.failure(e)
            }

            val serverTreatment = serverResponse.tratamiento
            if (serverTreatment == null) {
                Log.e(TAG, "Treatment object in server response is null")
                return@withContext Result.failure(Exception("Treatment object is null"))
            }

            val tipoStr = serverTreatment.tipo ?: "DIABETES"
            val treatmentType = try {
                TreatmentType.valueOf(tipoStr.uppercase())
            } catch (e: Exception) {
                TreatmentType.DIABETES
            }

            Log.d(TAG, "Starting download of files for treatment type: $treatmentType")

            val dbHelper = com.colma.colmaApp.servicios.database.dbhelper.DbHelper(context)
            val user = dbHelper.getUsuario(userId)
            val paisNombre = user?.pais?.nombre?.lowercase() ?: "argentina"
            val countryCode = if (paisNombre.contains("peru") || paisNombre.contains("perú")) "PE" else "AR"

            // Load static content.json from assets to merge quizzes and metas
            val assetContentDict: com.colma.colmaApp.features.content.model.StaticContentCatalog? = try {
                val jsonText = context.assets.open("content.json").bufferedReader().use { it.readText() }
                Json { ignoreUnknownKeys = true }.decodeFromString<com.colma.colmaApp.features.content.model.StaticContentCatalog>(jsonText)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load static content.json from assets: ${e.message}")
                null
            }
            // Load static treatments for merging quizzes/metas
            val countryTreatments = assetContentDict?.countries?.get(countryCode) ?: emptyList()
            val staticTreatments = listOfNotNull(countryTreatments.find { it.id == treatmentType })

            // Calculate total files to download first to report accurate progress
            val semanas = serverTreatment.semanas ?: emptyList()
            logAllAvailableServerResources(serverTreatment)
            var totalFiles = 0
            for (semana in semanas) {
                val sesiones = semana.sesiones ?: emptyList()
                for (sesion in sesiones) {
                    val serverContents = sesion.content ?: emptyList()
                    totalFiles += serverContents.sumOf { it.getDownloadableResources().size }
                }
            }
            Log.d(TAG, "Total files to download: $totalFiles")

            // Iterate and download all files, building the local structure
            val mappedSessions = mutableListOf<SessionContent>()
            var downloadedCount = 0

            for ((weekIndex, serverSemana) in semanas.withIndex()) {
                val staticSessions = staticTreatments.mapNotNull { it.sessions.getOrNull(weekIndex) }
                val staticSession = staticSessions.firstOrNull()
                val mappedSubSessions = mutableListOf<SubSessionContent>()

                val sesiones = serverSemana.sesiones ?: emptyList()
                for ((sessionIndex, serverSesion) in sesiones.withIndex()) {
                    val staticSubSessions = staticSessions.mapNotNull { it.subSessions.getOrNull(sessionIndex) }
                    val staticSubSession = staticSubSessions.firstOrNull()
                    val serverContents = serverSesion.content ?: emptyList()

                    // Download files and map paths
                    val downloadedFiles = mutableListOf<ServerFileMapping>()
                    for (contentItem in serverContents) {
                        val resources = contentItem.getDownloadableResources()
                        for (resource in resources) {
                            val link = resource.url
                            val parsedUri = Uri.parse(link)
                            val fileName = parsedUri.lastPathSegment ?: "${resource.type.lowercase()}_${contentItem.elementoId}"
                            val relativePath = parsedUri.path?.removePrefix("/") ?: "multimedia/${tipoStr}/${serverSemana.id}/${serverSesion.id}/${fileName}"
                            val localFile = File(context.filesDir, relativePath)

                            val currentProgress = (0.1f + 0.8f * (downloadedCount.toFloat() / maxOf(1, totalFiles))).coerceIn(0.1f, 0.90f)
                            onProgress?.invoke(currentProgress, "Descargando: ${resource.resourceName}")

                            // Download the file
                            Log.d(TAG, "Downloading file (${resource.type}): ${resource.resourceName} from $link")
                            val fileResponse = treatmentService.downloadFile(token, link)
                            Log.d(TAG, "File download response code for ${resource.resourceName}: ${fileResponse.code()}")
                            if (fileResponse.isSuccessful) {
                                val body = fileResponse.body()
                                if (body != null) {
                                    try {
                                        localFile.parentFile?.mkdirs()
                                        body.byteStream().use { inputStream ->
                                            FileOutputStream(localFile).use { outputStream ->
                                                inputStream.copyTo(outputStream)
                                            }
                                        }
                                        Log.d(TAG, "Successfully saved file to: ${localFile.absolutePath}")
                                        downloadedFiles.add(
                                            ServerFileMapping(
                                                type = resource.type,
                                                title = resource.resourceName,
                                                localPath = localFile.absolutePath,
                                                tratamientoMultimediaId = contentItem.elementoId
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error writing file ${resource.resourceName} to disk: ${e.message}")
                                        return@withContext Result.failure(e)
                                    }
                                }
                            } else {
                                if (isMockTestUser) {
                                    Log.w(TAG, "Mock test user: File download returned ${fileResponse.code()} for ${resource.resourceName}, continuing with mock placeholder.")
                                    localFile.parentFile?.mkdirs()
                                    if (!localFile.exists()) {
                                        localFile.writeBytes(ByteArray(0))
                                    }
                                    downloadedFiles.add(
                                        ServerFileMapping(
                                            type = resource.type,
                                            title = resource.resourceName,
                                            localPath = localFile.absolutePath,
                                            tratamientoMultimediaId = contentItem.elementoId
                                        )
                                    )
                                } else {
                                    val errorBody = fileResponse.errorBody()?.string() ?: ""
                                    Log.e(TAG, "Failed to download file ${resource.resourceName}: code=${fileResponse.code()}, error=$errorBody")
                                    return@withContext Result.failure(IOException("Failed to download file ${resource.resourceName}: code=${fileResponse.code()}, error=$errorBody"))
                                }
                            }
                            downloadedCount++
                        }
                    }

                    // Build mapped MultimediaContent list
                    val multimediaList = mutableListOf<MultimediaContent>()

                    val audioItems = downloadedFiles.filter { 
                        it.type.equals("AUDIO", ignoreCase = true) || 
                        it.title.contains("audio", ignoreCase = true) || 
                        it.title.endsWith(".mp3", ignoreCase = true) || 
                        it.title.endsWith(".wav", ignoreCase = true) || 
                        it.title.endsWith(".m4a", ignoreCase = true) || 
                        it.title.endsWith(".aac", ignoreCase = true) || 
                        it.title.endsWith(".ogg", ignoreCase = true)
                    }
                    val imageItems = downloadedFiles.filter { 
                        it.type.equals("IMAGE", ignoreCase = true) || 
                        it.type.equals("IMAGEN", ignoreCase = true) || 
                        it.title.contains("imagen", ignoreCase = true) || 
                        it.title.contains("image", ignoreCase = true) || 
                        it.title.contains("img", ignoreCase = true) || 
                        it.title.endsWith(".png", ignoreCase = true) || 
                        it.title.endsWith(".jpg", ignoreCase = true) || 
                        it.title.endsWith(".jpeg", ignoreCase = true) || 
                        it.title.endsWith(".webp", ignoreCase = true) ||
                        it.title.endsWith(".gif", ignoreCase = true)
                    }
                    val videoItems = downloadedFiles.filter { 
                        it.type.equals("VIDEO", ignoreCase = true) || 
                        it.title.contains("video", ignoreCase = true) || 
                        it.title.endsWith(".mp4", ignoreCase = true) || 
                        it.title.endsWith(".mov", ignoreCase = true) || 
                        it.title.endsWith(".mkv", ignoreCase = true) || 
                        it.title.endsWith(".webm", ignoreCase = true)
                    }

                    if (serverContents.isNotEmpty()) {
                        for ((itemIndex, serverItem) in serverContents.withIndex()) {
                            val itemType = try {
                                MultimediaContentType.valueOf(serverItem.type?.uppercase() ?: "DESCRIPTION")
                            } catch (e: Exception) {
                                MultimediaContentType.DESCRIPTION
                            }
                            val staticItem = staticSubSession?.multimedia?.find { 
                                it.type == itemType && (it.title.equals(serverItem.title, ignoreCase = true) || (it.metasId != null && it.metasId == serverItem.metasId))
                            } ?: staticSubSession?.multimedia?.getOrNull(itemIndex)

                            when (itemType) {
                                MultimediaContentType.DESCRIPTION -> {
                                    val matchedAudios = downloadedFiles.filter { 
                                        it.tratamientoMultimediaId == serverItem.elementoId && it.type.equals("AUDIO", ignoreCase = true) 
                                    }
                                    val matchedImages = downloadedFiles.filter { 
                                        it.tratamientoMultimediaId == serverItem.elementoId && it.type.equals("IMAGE", ignoreCase = true) 
                                    }

                                    val localAudios = if (matchedAudios.isNotEmpty()) {
                                        matchedAudios.map { it.localPath }
                                    } else {
                                        val fallbackAudio = audioItems.find { it.tratamientoMultimediaId == serverItem.elementoId }
                                            ?: (if (serverItem.title?.contains("Lectura", ignoreCase = true) == true) audioItems.firstOrNull() else null)
                                        if (fallbackAudio != null) listOf(fallbackAudio.localPath) else null
                                    }

                                    val localImages = if (matchedImages.isNotEmpty()) {
                                        matchedImages.map { it.localPath }
                                    } else {
                                        val fallbackImage = imageItems.find { it.tratamientoMultimediaId == serverItem.elementoId }
                                            ?: (if (serverItem.title?.contains("Lectura", ignoreCase = true) == true) imageItems.firstOrNull() else null)
                                        if (fallbackImage != null) listOf(fallbackImage.localPath) else null
                                    }

                                    multimediaList.add(
                                        MultimediaContent(
                                            type = MultimediaContentType.DESCRIPTION,
                                            title = serverItem.title ?: staticItem?.title ?: "Descripción",
                                            description = serverItem.description ?: staticItem?.description ?: "",
                                            fullDescription = serverItem.fullDescription ?: staticItem?.fullDescription ?: serverItem.description ?: "",
                                            iconResId = serverItem.iconResId ?: staticItem?.iconResId ?: "outline_unknown_document_24",
                                            iconImageVector = serverItem.iconImageVector ?: staticItem?.iconImageVector,
                                            contentAsset = null,
                                            metasId = serverItem.metasId ?: staticItem?.metasId,
                                            metasOptions = serverItem.metasOptions ?: staticItem?.metasOptions,
                                            questions = serverItem.questions ?: staticItem?.questions,
                                            descriptionAudios = localAudios,
                                            descriptionImages = localImages,
                                            tratamientoMultimediaId = serverItem.elementoId,
                                            tratamientoElementoId = serverItem.elementoId
                                        )
                                    )
                                }
                                MultimediaContentType.VIDEO -> {
                                    val videoItem = downloadedFiles.find { it.tratamientoMultimediaId == serverItem.elementoId && it.type.equals("VIDEO", ignoreCase = true) }
                                        ?: videoItems.find { it.tratamientoMultimediaId == serverItem.elementoId }
                                        ?: videoItems.find { it.title == serverItem.title }
                                        ?: videoItems.firstOrNull()
                                    multimediaList.add(
                                        MultimediaContent(
                                            type = MultimediaContentType.VIDEO,
                                            title = serverItem.title ?: staticItem?.title ?: "Video",
                                            description = serverItem.description ?: staticItem?.description ?: "Ver video de la sesión",
                                            fullDescription = serverItem.fullDescription ?: staticItem?.fullDescription,
                                            iconResId = serverItem.iconResId ?: staticItem?.iconResId,
                                            iconImageVector = serverItem.iconImageVector ?: staticItem?.iconImageVector ?: "PlayArrow",
                                            contentAsset = videoItem?.localPath ?: serverItem.video ?: serverItem.effectiveLink,
                                            metasId = serverItem.metasId ?: staticItem?.metasId,
                                            metasOptions = serverItem.metasOptions ?: staticItem?.metasOptions,
                                            questions = serverItem.questions ?: staticItem?.questions,
                                            descriptionAudios = null,
                                            descriptionImages = null,
                                            tratamientoMultimediaId = serverItem.elementoId,
                                            tratamientoElementoId = serverItem.elementoId
                                        )
                                    )
                                }
                                MultimediaContentType.QUIZ -> {
                                    multimediaList.add(
                                        MultimediaContent(
                                            type = MultimediaContentType.QUIZ,
                                            title = serverItem.title ?: staticItem?.title ?: "Pregunta del día",
                                            description = serverItem.description ?: staticItem?.description ?: "Responde las preguntas de la sesión",
                                            fullDescription = serverItem.fullDescription ?: staticItem?.fullDescription,
                                            iconResId = serverItem.iconResId ?: staticItem?.iconResId ?: "outline_trophy_24",
                                            iconImageVector = serverItem.iconImageVector ?: staticItem?.iconImageVector,
                                            contentAsset = null,
                                            metasId = serverItem.metasId ?: staticItem?.metasId,
                                            metasOptions = serverItem.metasOptions ?: staticItem?.metasOptions,
                                            questions = serverItem.questions ?: staticItem?.questions,
                                            descriptionAudios = null,
                                            descriptionImages = null,
                                            tratamientoMultimediaId = serverItem.elementoId,
                                            tratamientoElementoId = serverItem.elementoId
                                        )
                                    )
                                }
                                MultimediaContentType.METAS -> {
                                    multimediaList.add(
                                        MultimediaContent(
                                            type = MultimediaContentType.METAS,
                                            title = serverItem.title ?: staticItem?.title ?: "Metas",
                                            description = serverItem.description ?: staticItem?.description ?: "Planifica tus metas",
                                            fullDescription = serverItem.fullDescription ?: staticItem?.fullDescription,
                                            iconResId = serverItem.iconResId ?: staticItem?.iconResId ?: "outline_trophy_24",
                                            iconImageVector = serverItem.iconImageVector ?: staticItem?.iconImageVector,
                                            contentAsset = null,
                                            metasId = serverItem.metasId ?: staticItem?.metasId ?: "1",
                                            metasOptions = serverItem.metasOptions ?: staticItem?.metasOptions,
                                            questions = null,
                                            descriptionAudios = null,
                                            descriptionImages = null,
                                            tratamientoMultimediaId = serverItem.elementoId,
                                            tratamientoElementoId = serverItem.elementoId
                                        )
                                    )
                                }
                                else -> {
                                    multimediaList.add(
                                        MultimediaContent(
                                            type = itemType,
                                            title = serverItem.title ?: staticItem?.title ?: "",
                                            description = serverItem.description ?: staticItem?.description ?: "",
                                            fullDescription = serverItem.fullDescription ?: staticItem?.fullDescription,
                                            iconResId = serverItem.iconResId ?: staticItem?.iconResId,
                                            iconImageVector = serverItem.iconImageVector ?: staticItem?.iconImageVector,
                                            contentAsset = null,
                                            metasId = serverItem.metasId ?: staticItem?.metasId,
                                            metasOptions = serverItem.metasOptions ?: staticItem?.metasOptions,
                                            questions = serverItem.questions ?: staticItem?.questions,
                                            tratamientoMultimediaId = serverItem.elementoId,
                                            tratamientoElementoId = serverItem.elementoId
                                        )
                                    )
                                }
                            }
                        }
                    } else if (staticSubSession != null) {
                        multimediaList.addAll(staticSubSession.multimedia)
                    }

                    mappedSubSessions.add(
                        SubSessionContent(
                            id = (serverSesion.id ?: (sessionIndex + 1)).toLong(),
                            title = serverSesion.title ?: "Sesión ${sessionIndex + 1}",
                            duration = staticSubSession?.duration ?: "05:00",
                            multimedia = multimediaList
                        )
                    )
                }

                mappedSessions.add(
                    SessionContent(
                        id = (serverSemana.id ?: (weekIndex + 1)).toLong(),
                        title = serverSemana.title ?: "Semana ${weekIndex + 1}",
                        thumbnail = staticSession?.thumbnail ?: "missing_photo",
                        duration = staticSession?.duration ?: "10 min",
                        subSessions = mappedSubSessions
                    )
                )
            }

            val mappedTreatment = TreatmentContent(
                id = treatmentType,
                name = serverTreatment.nombre ?: tipoStr,
                country = countryCode,
                sessions = mappedSessions
            )

            onProgress?.invoke(0.95f, "Guardando datos localmente...")

            // Save JSON standard representation locally
            val localContentDict = ContentDictionary(treatments = listOf(mappedTreatment))
            val jsonText = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(localContentDict)

            val localJsonFile = File(context.filesDir, "treatment_download_${userId}.json")
            localJsonFile.writeText(jsonText)
            Log.d(TAG, "Successfully saved treatment_download_${userId}.json: ${localJsonFile.absolutePath}")

            // Fetch completed multimedia/elementos from server to restore user's progress
            try {
                onProgress?.invoke(0.98f, "Restaurando progreso previo...")
                val completadosResponse = try {
                    treatmentService.getElementosCompletados(token)
                } catch (e: Exception) {
                    null
                }
                val completedIds = completadosResponse?.body()?.elementosCompletados?.toSet()
                    ?: try {
                        treatmentService.getMultimediaCompletados(token).body()?.completedIds?.toSet()
                    } catch (e: Exception) {
                        null
                    } ?: emptySet()

                if (completedIds.isNotEmpty()) {
                    for (session in mappedSessions) {
                        val weekId = session.id.toInt()
                        for (subSession in session.subSessions) {
                            val subSessionId = subSession.id.toInt()
                            val processedContents = subSession.multimedia
                            val size = processedContents.size
                            var allCompleted = processedContents.isNotEmpty()

                            for ((index, media) in processedContents.withIndex()) {
                                val mediaId = media.elementoId
                                if (mediaId != null && completedIds.contains(mediaId)) {
                                    val isLocalFile = media.contentAsset?.contains("/") == true || media.contentAsset?.contains("\\") == true
                                    val localFilePath = if (isLocalFile) media.contentAsset else null
                                    val contentAssetId = if (isLocalFile) null else media.contentAsset?.let {
                                        val resId = com.colma.colmaApp.utils.ResourceUtils.getResourceId(context, it, "raw")
                                        if (resId != 0) resId else null
                                    }

                                    val originalTitle = media.title.trim()
                                    val finalTitle = if (originalTitle.isNotEmpty()) {
                                        originalTitle
                                    } else if (media.type == MultimediaContentType.DESCRIPTION) {
                                        if (index == 0) {
                                            "Bienvenida"
                                        } else if (index == size - 1 && size > 1) {
                                            "Fin de la sesión"
                                        } else {
                                            "Descripción"
                                        }
                                    } else {
                                        media.type.name
                                    }

                                    val uniqueKey = MultimediaKeyUtils.generateMultimediaUniqueKey(
                                        type = media.type,
                                        title = finalTitle,
                                        contentAssetId = contentAssetId,
                                        metasId = media.metasId,
                                        treatmentId = treatmentType.name,
                                        weekId = weekId.toLong(),
                                        sessionId = subSessionId.toLong(),
                                        localFilePath = localFilePath
                                    )

                                    dbHelper.upsertMultimediaRead(
                                        idUsuario = userId,
                                        treatmentId = treatmentType.name,
                                        weekId = weekId,
                                        sessionId = subSessionId,
                                        multimediaType = media.type.name,
                                        multimediaUniqueKey = uniqueKey,
                                        tratamientoMultimediaId = mediaId,
                                        tratamientoElementoId = mediaId
                                    )
                                } else {
                                    if (media.elementoId != null) {
                                        allCompleted = false
                                    }
                                }
                            }

                            if (allCompleted) {
                                dbHelper.insertCompletedSession(
                                    idUsuario = userId,
                                    treatmentId = treatmentType.name,
                                    weekId = weekId,
                                    sessionId = subSessionId
                                )
                            }
                        }
                    }
                    Log.d(TAG, "Restaurados ${completedIds.size} elementos completados desde el servidor.")
                }

                // Restore functional data (quizzes and metas)
                try {
                    val functionalResponse = treatmentService.getDatosFuncionalesTratamiento(token)
                    if (functionalResponse.isSuccessful) {
                        val funcBody = functionalResponse.body()
                        val serverQuizzes = funcBody?.quiz ?: emptyList()
                        val serverMetas = funcBody?.metas ?: emptyList()

                        for (sq in serverQuizzes) {
                            val elemId = sq.tratamientoElementoId
                            if (elemId != null && !dbHelper.hasQuizResponseForElement(userId, elemId)) {
                                val qRecord = QuizResponseRecord(
                                    id_usuario = userId,
                                    quiz_type = sq.quizType,
                                    week_id = sq.sessionId,
                                    session_id = sq.subSessionId,
                                    quiz_title = "Quiz",
                                    fecha_registro = sq.fechaRegistro,
                                    fecha_sincronizacion = sq.fechaSincronizacion,
                                    total_questions = sq.preguntas.size,
                                    correct_answers = sq.preguntas.count { it.esCorrecta },
                                    percentage_correct = if (sq.preguntas.isNotEmpty()) (sq.preguntas.count { it.esCorrecta }.toDouble() / sq.preguntas.size) * 100 else 0.0,
                                    multimedia_unique_key = "QUIZ_${treatmentType.name}_${elemId}",
                                    tratamiento_elemento_id = elemId
                                )
                                val qResponses = sq.preguntas.mapIndexed { idx, p ->
                                    com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord(
                                        id_quiz_response = null,
                                        question_index = p.preguntaIndex,
                                        question_text = p.preguntaTexto,
                                        user_answer_index = p.respuestaUsuario,
                                        user_answer_text = p.respuestaUsuarioTexto,
                                        correct_answer_index = p.respuestaCorrecta,
                                        correct_answer_text = p.respuestaCorrectaTexto,
                                        is_correct = if (p.esCorrecta) 1 else 0,
                                        posibles_respuestas = Gson().toJson(p.posiblesRespuestas)
                                    )
                                }
                                dbHelper.insertQuizResponse(qRecord, qResponses)
                            }
                        }

                        for (sm in serverMetas) {
                            val existing = dbHelper.getMetaById(sm.idMeta.toLong())
                            if (existing == null) {
                                val metaRecord = RegistrosSaludModelos.MetaRecord(
                                    id_meta = sm.idMeta.toLong(),
                                    id_usuario = userId,
                                    semana = sm.semana,
                                    sesion = sm.sesion,
                                    tipo_actividad = sm.tipoActividad,
                                    actividad = sm.actividad,
                                    lugar = sm.lugar,
                                    acompañante = sm.acompaniante,
                                    fecha_regisitro = sm.fechaRegistro,
                                    fecha_edicion = sm.fechaEdicion,
                                    fecha_sincronizacion = sm.fechaSincronizacion,
                                    meta_cumplida = if (sm.metaCumplida) 2 else if (!sm.motivoIncumplimiento.isNullOrBlank()) 1 else 0,
                                    fecha_objetivo = sm.fechaObjetivo,
                                    motivo_incumplimiento = sm.motivoIncumplimiento,
                                    tratamiento_elemento_id = sm.tratamientoElementoId
                                )
                                dbHelper.insertMeta(metaRecord)
                            }
                        }
                        Log.d(TAG, "Restaurados datos funcionales: ${serverQuizzes.size} quizzes, ${serverMetas.size} metas.")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while restoring functional data: ${e.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception while restoring completed multimedia: ${e.message}")
            }


            onProgress?.invoke(1.0f, "Completado")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in downloadAndSaveTreatment: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun isTreatmentDownloaded(userId: Int): Boolean = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "treatment_download_${userId}.json")
        file.exists() && file.length() > 0
    }

    override suspend fun clearDownloadedTreatment(userId: Int): Unit = withContext(Dispatchers.IO) {
        try {
            // Delete treatment_download_${userId}.json
            val file = File(context.filesDir, "treatment_download_${userId}.json")
            if (file.exists()) {
                file.delete()
            }
            Log.d(TAG, "Successfully cleared downloaded treatment JSON for user $userId.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing downloaded treatment: ${e.message}")
        }
    }

    private fun extractIndex(title: String): Int? {
        val regexNamed = Regex("""(?i)(?:audio|imagen|image|img|video)[-_ ]*(?:dm|hta)?[-_ ]*(\d+)""")
        val matchNamed = regexNamed.find(title)
        if (matchNamed != null) {
            return matchNamed.groupValues[1].toIntOrNull()
        }
        val separatorIndex = title.indexOfAny(charArrayOf('-', '_'))
        if (separatorIndex != -1) {
            val rest = title.substring(separatorIndex + 1)
            val digits = rest.filter { it.isDigit() }
            if (digits.isNotEmpty()) {
                return digits.toIntOrNull()
            }
        }
        val anyDigits = title.filter { it.isDigit() }
        if (anyDigits.isNotEmpty()) {
            return anyDigits.toIntOrNull()
        }
        return null
    }

    private data class ServerFileMapping(
        val type: String,
        val title: String,
        val localPath: String,
        val tratamientoMultimediaId: Int? = null
    )

    private fun logAllAvailableServerResources(serverTreatment: ServerTreatment) {
        Log.d(TAG, "==================================================")
        Log.d(TAG, "=== LISTADO DE RECURSOS DEL SERVIDOR (TRATAMIENTO: ${serverTreatment.tipo ?: serverTreatment.nombre}) ===")
        Log.d(TAG, "==================================================")
        val semanas: List<ServerSemana> = serverTreatment.semanas ?: emptyList()
        for (semana in semanas) {
            val semanaTitle = semana.title ?: "Semana ${semana.id}"
            val sesiones: List<ServerSesion> = semana.sesiones ?: emptyList()
            for (sesion in sesiones) {
                val sesionTitle = sesion.title ?: "Sesión ${sesion.id}"
                val contents: List<ServerContentItem> = sesion.content ?: emptyList()
                val images = contents.filter { item: ServerContentItem ->
                    item.type.equals("IMAGEN", true) || 
                    item.type.equals("IMAGE", true) || 
                    item.title?.contains("imagen", true) == true || 
                    item.title?.contains("image", true) == true || 
                    item.title?.contains("img", true) == true ||
                    item.title?.endsWith(".png", true) == true ||
                    item.title?.endsWith(".jpg", true) == true ||
                    item.title?.endsWith(".webp", true) == true
                }
                val audios = contents.filter { item: ServerContentItem ->
                    item.type.equals("AUDIO", true) || 
                    item.title?.contains("audio", true) == true ||
                    item.title?.endsWith(".mp3", true) == true ||
                    item.title?.endsWith(".wav", true) == true ||
                    item.title?.endsWith(".m4a", true) == true
                }
                val videos = contents.filter { item: ServerContentItem ->
                    item.type.equals("VIDEO", true) || 
                    item.title?.contains("video", true) == true ||
                    item.title?.endsWith(".mp4", true) == true
                }
                
                Log.d(TAG, "[$semanaTitle - $sesionTitle] -> Total items: ${contents.size} (Imágenes: ${images.size}, Audios: ${audios.size}, Videos: ${videos.size})")
                for (c in contents) {
                    Log.d(TAG, "    * [${c.type ?: "N/A"}] ${c.title ?: "Sin título"} -> ${c.link ?: "Sin enlace"}")
                }
            }
        }
        Log.d(TAG, "==================================================")
    }
}
