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
import com.colma.colmaApp.servicios.internetCommunication.model.ServerTreatmentResponse
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

            // Load static content.json from assets to merge quizzes and metas
            val assetContentDict: ContentDictionary? = try {
                val jsonText = context.assets.open("content.json").bufferedReader().use { it.readText() }
                Json { ignoreUnknownKeys = true }.decodeFromString<ContentDictionary>(jsonText)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load static content.json from assets: ${e.message}")
                null
            }
            val staticTreatment = assetContentDict?.treatments?.find { it.id == treatmentType }

            // Calculate total files to download first to report accurate progress
            val semanas = serverTreatment.semanas ?: emptyList()
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
                val staticSession = staticTreatment?.sessions?.getOrNull(weekIndex)
                val mappedSubSessions = mutableListOf<SubSessionContent>()

                val sesiones = serverSemana.sesiones ?: emptyList()
                for ((sessionIndex, serverSesion) in sesiones.withIndex()) {
                    val staticSubSession = staticSession?.subSessions?.getOrNull(sessionIndex)
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

                    // Filter out reading audios/images to group them
                    val audioItems = downloadedFiles.filter { it.title.contains("audio-", ignoreCase = true) }
                    val imageItems = downloadedFiles.filter { it.title.contains("imagen-", ignoreCase = true) }
                    val videoItems = downloadedFiles.filter { it.title.contains("video-", ignoreCase = true) }

                    // Determine number of readings (max index)
                    val maxIndex = maxOf(
                        audioItems.mapNotNull { extractIndex(it.title) }.maxOrNull() ?: 0,
                        imageItems.mapNotNull { extractIndex(it.title) }.maxOrNull() ?: 0
                    )

                    if (maxIndex > 0) {
                        val staticDescription = staticSubSession?.multimedia?.find { it.type == MultimediaContentType.DESCRIPTION }
                        val localAudios = mutableListOf<String>()
                        val localImages = mutableListOf<String>()

                        for (i in 1..maxIndex) {
                            val audioItem = audioItems.find { extractIndex(it.title) == i }
                            val imageItem = imageItems.find { extractIndex(it.title) == i }

                            if (audioItem != null) {
                                localAudios.add(audioItem.localPath)
                            }
                            if (imageItem != null) {
                                localImages.add(imageItem.localPath)
                            }
                        }

                        multimediaList.add(
                            MultimediaContent(
                                type = MultimediaContentType.DESCRIPTION,
                                title = staticDescription?.title ?: "Descripción de la sesión",
                                description = staticDescription?.description ?: "Lee y escucha la sesión del día",
                                fullDescription = staticDescription?.fullDescription ?: "",
                                iconResId = staticDescription?.iconResId ?: "outline_unknown_document_24",
                                iconImageVector = staticDescription?.iconImageVector,
                                contentAsset = null,
                                metasId = staticDescription?.metasId,
                                questions = staticDescription?.questions,
                                descriptionAudios = localAudios,
                                descriptionImages = localImages
                            )
                        )
                    }

                    // Add video items
                    videoItems.forEach { videoItem ->
                        val staticVideo = staticSubSession?.multimedia?.find { it.type == MultimediaContentType.VIDEO }
                        multimediaList.add(
                            MultimediaContent(
                                type = MultimediaContentType.VIDEO,
                                title = staticVideo?.title ?: videoItem.title,
                                description = staticVideo?.description ?: "Ver video de la sesión",
                                fullDescription = staticVideo?.fullDescription,
                                iconResId = staticVideo?.iconResId,
                                iconImageVector = staticVideo?.iconImageVector ?: "PlayArrow",
                                contentAsset = videoItem.localPath,
                                metasId = staticVideo?.metasId,
                                questions = staticVideo?.questions,
                                descriptionAudios = null,
                                descriptionImages = null
                            )
                        )
                    }

                    // Merge static QUIZ and METAS
                    staticSubSession?.multimedia?.filter {
                        it.type == MultimediaContentType.QUIZ || it.type == MultimediaContentType.METAS
                    }?.forEach { staticItem ->
                        if (multimediaList.none { it.type == staticItem.type }) {
                            multimediaList.add(staticItem)
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
        val hyphenIndex = title.indexOf('-')
        if (hyphenIndex != -1) {
            val rest = title.substring(hyphenIndex + 1)
            val digits = rest.takeWhile { it.isDigit() }
            if (digits.isNotEmpty()) {
                return digits.toIntOrNull()
            }
        }
        return null
    }

    private data class ServerFileMapping(
        val type: String,
        val title: String,
        val localPath: String
    )
}
