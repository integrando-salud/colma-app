package com.colma.colmaApp.features.content.data

import android.content.Context
import android.util.Log
import com.colma.colmaApp.features.content.domain.ContentRepository
import com.colma.colmaApp.features.content.domain.SubSessionProgress
import com.colma.colmaApp.features.content.model.ContentDictionary
import com.colma.colmaApp.features.content.model.MultimediaContent
import com.colma.colmaApp.features.content.model.SessionContent
import com.colma.colmaApp.features.content.model.SubSessionContent
import com.colma.colmaApp.features.content.model.TreatmentContent
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ContentRepositoryImpl(private val context: Context) : ContentRepository {
    private val TAG = "ContentRepository"

    private suspend fun loadContent(): ContentDictionary {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Load downloaded local content if exists
                val userId = com.colma.colmaApp.servicios.session.SessionManager.getUserId()
                val localFile = if (userId != null) {
                    File(context.filesDir, "treatment_download_${userId}.json")
                } else {
                    File(context.filesDir, "treatment_download.json")
                }
                if (localFile.exists() && localFile.length() > 0) {
                    try {
                        val localJsonText = localFile.readText()
                        val localContent = Json { ignoreUnknownKeys = true }.decodeFromString<ContentDictionary>(localJsonText)
                        Log.d(TAG, "Successfully loaded user-specific downloaded treatments.")
                        return@withContext localContent
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading treatment_download.json: ${e.message}", e)
                    }
                }

                // 2. Fallback to static asset content
                val assetJsonText = context.assets.open("content.json").bufferedReader().use { it.readText() }
                Json { ignoreUnknownKeys = true }.decodeFromString<ContentDictionary>(assetJsonText)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading content: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun getAllTreatments(): Result<List<TreatmentContent>> {
        return try {
            val dictionary = loadContent()
            Log.d(TAG, "Successfully loaded ${dictionary.treatments.size} treatments")
            Result.success(dictionary.treatments)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAllTreatments: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getSubSessions(treatmentType: TreatmentType, sessionId: Long): Result<List<SubSessionContent>> {
        return try {
            val dictionary = loadContent()
            val sessions = dictionary.treatments.find { it.id == treatmentType }?.sessions ?: emptyList()
            val subSessions = sessions.find { it.id == sessionId }?.subSessions ?: emptyList()
            Result.success(subSessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMultimediaOptions(treatmentType: TreatmentType, sessionId: Long, subSessionId: Long): Result<List<MultimediaContent>> {
        return try {
            val dictionary = loadContent()
            val treatment = dictionary.treatments.find { it.id == treatmentType }
            
            // First find the specific session (week), then find the subSession within it
            val session = treatment?.sessions?.find { it.id == sessionId }
            val subSession = session?.subSessions?.find { it.id == subSessionId }
            val multimediaOptions = subSession?.multimedia ?: emptyList()
            
            Result.success(multimediaOptions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSubSessionProgress(
        treatmentType: TreatmentType,
        sessionId: Long,
        subSessionId: Long
    ): Result<SubSessionProgress> {
        return try {
            val dictionary = loadContent()
            val treatment = dictionary.treatments.find { it.id == treatmentType }
            val session = treatment?.sessions?.find { it.id == sessionId }
            val subSessions = session?.subSessions ?: emptyList()
            val currentIndex = subSessions.indexOfFirst { it.id == subSessionId }
            val totalCount = subSessions.size
            if (currentIndex >= 0 && totalCount > 0) {
                Result.success(SubSessionProgress(currentIndex = currentIndex + 1, totalCount = totalCount))
            } else {
                Result.failure(Exception("Sub-session not found in session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getResourceId(resourceName: String, resourceType: String): Int {
        return context.resources.getIdentifier(resourceName, resourceType, context.packageName)
    }
}
