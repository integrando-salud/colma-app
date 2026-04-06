package com.colma.colmaApp.features.content.data

import android.content.Context
import com.colma.colmaApp.features.content.domain.ContentRepository
import com.colma.colmaApp.features.content.domain.SubSessionProgress
import com.colma.colmaApp.features.content.model.ContentDictionary
import com.colma.colmaApp.features.content.model.MultimediaContent
import com.colma.colmaApp.features.content.model.SessionContent
import com.colma.colmaApp.features.content.model.SubSessionContent
import com.colma.colmaApp.features.content.model.TreatmentContent
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentRepositoryImpl(private val context: Context) : ContentRepository {

    private var content: ContentDictionary? = null

    private suspend fun loadContent(): ContentDictionary {
        if (content == null) {
            content = withContext(Dispatchers.IO) {
                context.assets.open("content.json").bufferedReader().use {
                    Json.decodeFromString(it.readText())
                }
            }
        }
        return content!!
    }

    override suspend fun getAllTreatments(): Result<List<TreatmentContent>> {
        return try {
            val dictionary = loadContent()
            Result.success(dictionary.treatments)
        } catch (e: Exception) {
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

    override suspend fun getMultimediaOptions(treatmentType: TreatmentType, subSessionId: Long): Result<List<MultimediaContent>> {
        return try {
            val dictionary = loadContent()
            val sessions = dictionary.treatments.find { it.id == treatmentType }?.sessions ?: emptyList()
            val subSessions = sessions.flatMap { it.subSessions }
            val multimediaOptions = subSessions.find { it.id == subSessionId }?.multimedia ?: emptyList()
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
