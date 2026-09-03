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
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TreatmentDownloadRepositoryImpl(private val context: Context) : TreatmentDownloadRepository {

    private val TAG = "TreatmentDownloadRepo"
    private val treatmentService = TreatmentService()

    override suspend fun downloadAndSaveTreatment(
        token: String,
        userId: Int,
        onProgress: ((progress: Float, status: String) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(0.05f, "Conectando al servidor...")
            Log.d(TAG, "Fetching treatment from server...")
            val response = treatmentService.getTratamiento(token)
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to fetch treatment: code=${response.code()}, error=$errorMsg")
                return@withContext Result.failure(IOException("Failed to fetch treatment: ${response.code()}"))
            }

            val bodyString = response.body()?.string() ?: ""
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
                    totalFiles += serverContents.count { it.title != null && it.link != null }
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
                        val title = contentItem.title ?: continue
                        val link = contentItem.link ?: continue

                        // Determine local file path
                        val parsedUri = Uri.parse(link)
                        val relativePath = parsedUri.path?.removePrefix("/") ?: "multimedia/${tipoStr}/${serverSemana.id}/${serverSesion.id}/${title}"
                        val localFile = File(context.filesDir, relativePath)

                        val currentProgress = 0.1f + 0.8f * (downloadedCount.toFloat() / maxOf(1, totalFiles))
                        onProgress?.invoke(currentProgress, "Descargando: $title")

                        // Download the file
                        Log.d(TAG, "Downloading file: $title from $link")
                        val fileResponse = treatmentService.downloadFile(token, link)
                        Log.d(TAG, "File download response code for $title: ${fileResponse.code()}")
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
                                    downloadedFiles.add(ServerFileMapping(contentItem.type ?: "VIDEO", title, localFile.absolutePath))
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error writing file $title to disk: ${e.message}")
                                    return@withContext Result.failure(e)
                                }
                            }
                        } else {
                            val errorBody = fileResponse.errorBody()?.string() ?: ""
                            Log.e(TAG, "Failed to download file $title: code=${fileResponse.code()}, error=$errorBody")
                            return@withContext Result.failure(IOException("Failed to download file $title: code=${fileResponse.code()}, error=$errorBody"))
                        }
                        downloadedCount++
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
                        it.title.endsWith(".webp", ignoreCase = true)
                    }
                    val videoItems = downloadedFiles.filter { 
                        it.type.equals("VIDEO", ignoreCase = true) || 
                        it.title.contains("video", ignoreCase = true) || 
                        it.title.endsWith(".mp4", ignoreCase = true) || 
                        it.title.endsWith(".mov", ignoreCase = true) || 
                        it.title.endsWith(".mkv", ignoreCase = true) || 
                        it.title.endsWith(".webm", ignoreCase = true)
                    }

                    if (staticSubSession != null) {
                        val readingItems = staticSubSession.multimedia.filter { 
                            it.type == MultimediaContentType.DESCRIPTION && it.title.contains("Lectura", ignoreCase = true) 
                        }

                        staticSubSession.multimedia.forEach { staticItem ->
                            when (staticItem.type) {
                                MultimediaContentType.DESCRIPTION -> {
                                    if (staticItem.title.contains("Lectura", ignoreCase = true)) {
                                        val readingIndexInSubSession = readingItems.indexOf(staticItem)
                                        val digits = staticItem.title.filter { it.isDigit() }
                                        val targetIdx = digits.toIntOrNull() ?: (if (readingIndexInSubSession >= 0) readingIndexInSubSession + 1 else 1)

                                        val isDmReading = staticItem.title.contains("Diabetes", ignoreCase = true) || staticItem.title.contains("DM", ignoreCase = true)
                                        val isHtaReading = staticItem.title.contains("Hipertensión", ignoreCase = true) || staticItem.title.contains("HTA", ignoreCase = true)

                                        val audioItem = audioItems.find { item ->
                                            val itemIdx = extractIndex(item.title)
                                            val matchesIdx = itemIdx == targetIdx
                                            val matchesTag = if (isDmReading) item.title.contains("dm", ignoreCase = true) else if (isHtaReading) item.title.contains("hta", ignoreCase = true) else true
                                            matchesIdx && matchesTag
                                        } ?: audioItems.find { extractIndex(it.title) == targetIdx }
                                          ?: audioItems.find { item ->
                                              if (isDmReading) item.title.contains("dm", ignoreCase = true)
                                              else if (isHtaReading) item.title.contains("hta", ignoreCase = true)
                                              else false
                                          }
                                          ?: (if (readingIndexInSubSession >= 0) audioItems.getOrNull(readingIndexInSubSession) else null)
                                          ?: (if (readingItems.size == 1 && audioItems.size == 1) audioItems.firstOrNull() else null)

                                        val imageItem = imageItems.find { item ->
                                            val itemIdx = extractIndex(item.title)
                                            val matchesIdx = itemIdx == targetIdx
                                            val matchesTag = if (isDmReading) item.title.contains("dm", ignoreCase = true) else if (isHtaReading) item.title.contains("hta", ignoreCase = true) else true
                                            matchesIdx && matchesTag
                                        } ?: imageItems.find { extractIndex(it.title) == targetIdx }
                                          ?: imageItems.find { item ->
                                              if (isDmReading) item.title.contains("dm", ignoreCase = true)
                                              else if (isHtaReading) item.title.contains("hta", ignoreCase = true)
                                              else false
                                          }
                                          ?: (if (readingIndexInSubSession >= 0) imageItems.getOrNull(readingIndexInSubSession) else null)
                                          ?: (if (readingItems.size == 1 && imageItems.size == 1) imageItems.firstOrNull() else null)

                                        multimediaList.add(
                                            MultimediaContent(
                                                type = MultimediaContentType.DESCRIPTION,
                                                title = staticItem.title,
                                                description = staticItem.description,
                                                fullDescription = staticItem.fullDescription ?: "",
                                                iconResId = staticItem.iconResId ?: "outline_unknown_document_24",
                                                iconImageVector = staticItem.iconImageVector,
                                                contentAsset = null,
                                                metasId = staticItem.metasId,
                                                questions = staticItem.questions,
                                                descriptionAudios = if (audioItem != null) listOf(audioItem.localPath) else null,
                                                descriptionImages = if (imageItem != null) listOf(imageItem.localPath) else null
                                            )
                                        )
                                    } else {
                                        multimediaList.add(staticItem)
                                    }
                                }
                                MultimediaContentType.VIDEO -> {
                                    val videoIdx = staticSubSession.multimedia.filter { it.type == MultimediaContentType.VIDEO }.indexOf(staticItem) + 1
                                    val videoItem = videoItems.find { extractIndex(it.title) == videoIdx } ?: videoItems.firstOrNull()
                                    multimediaList.add(
                                        MultimediaContent(
                                            type = MultimediaContentType.VIDEO,
                                            title = staticItem.title,
                                            description = staticItem.description,
                                            fullDescription = staticItem.fullDescription,
                                            iconResId = staticItem.iconResId,
                                            iconImageVector = staticItem.iconImageVector ?: "PlayArrow",
                                            contentAsset = videoItem?.localPath,
                                            metasId = staticItem.metasId,
                                            questions = staticItem.questions,
                                            descriptionAudios = null,
                                            descriptionImages = null
                                        )
                                    )
                                }
                                MultimediaContentType.QUIZ, MultimediaContentType.METAS -> {
                                    multimediaList.add(staticItem)
                                }
                                else -> {
                                    multimediaList.add(staticItem)
                                }
                            }
                        }
                    } else {
                        // Fallback fallback loop if static content doesn't exist
                        val maxIndex = maxOf(
                            audioItems.mapNotNull { extractIndex(it.title) }.maxOrNull() ?: 0,
                            imageItems.mapNotNull { extractIndex(it.title) }.maxOrNull() ?: 0
                        )
                        if (maxIndex > 0) {
                            val localAudios = audioItems.map { it.localPath }
                            val localImages = imageItems.map { it.localPath }
                            multimediaList.add(
                                MultimediaContent(
                                    type = MultimediaContentType.DESCRIPTION,
                                    title = "Lectura",
                                    description = "Lee y escucha la sesión del día",
                                    fullDescription = "",
                                    iconResId = "outline_unknown_document_24",
                                    descriptionAudios = localAudios,
                                    descriptionImages = localImages
                                )
                            )
                        }
                        videoItems.forEach { videoItem ->
                            multimediaList.add(
                                MultimediaContent(
                                    type = MultimediaContentType.VIDEO,
                                    title = videoItem.title,
                                    description = "Ver video de la sesión",
                                    iconImageVector = "PlayArrow",
                                    contentAsset = videoItem.localPath
                                )
                            )
                        }
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
        val localPath: String
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
