package com.colma.colmaApp.features.sesiones.SesionesMenu.data

import android.content.Context
import android.util.Log
import com.colma.colmaApp.features.content.domain.ContentRepository
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SesionesRepository
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SessionItem
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SubSessionItem
import com.colma.colmaApp.utils.ResourceUtils
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.session.SessionManager

class SesionesRepositoryImpl(
    private val contentRepository: ContentRepository,
    private val context: Context,
    private val dbHelper: DbHelper
) : SesionesRepository {
    private val TAG = "SesionesRepository"
    private val userId: Int

    init {
        SessionManager.init(context.applicationContext)
        userId = SessionManager.getUserId() ?: 1
    }

    override suspend fun getHtaSessions(): List<SessionItem> {
        return contentRepository.getAllTreatments().fold(
            onSuccess = { treatmentContents ->
                Log.d(TAG, "Successfully loaded ${treatmentContents.size} treatments")
                treatmentContents
                    .filter { it.id == TreatmentType.HTA }
                    .flatMap { it.sessions }
                    .map { sessionContent ->
                        SessionItem(
                            id = sessionContent.id,
                            title = sessionContent.title,
                            treatmentType = TreatmentType.HTA,
                            thumbnailRes = ResourceUtils.getResourceId(context, sessionContent.thumbnail, "drawable"),
                            duration = sessionContent.duration
                        )
                    }
            },
            onFailure = { error ->
                Log.e(TAG, "Error loading HTA sessions: ${error.message}", error)
                emptyList()
            }
        )
    }

    override suspend fun getDiabetesSessions(): List<SessionItem> {
        return contentRepository.getAllTreatments().fold(
            onSuccess = { treatmentContents ->
                Log.d(TAG, "Successfully loaded ${treatmentContents.size} treatments")
                treatmentContents
                    .filter { it.id == TreatmentType.DIABETES }
                    .flatMap { it.sessions }
                    .map { sessionContent ->
                        SessionItem(
                            id = sessionContent.id,
                            title = sessionContent.title,
                            treatmentType = TreatmentType.DIABETES,
                            thumbnailRes = ResourceUtils.getResourceId(context, sessionContent.thumbnail, "drawable"),
                            duration = sessionContent.duration
                        )
                    }
            },
            onFailure = { error ->
                Log.e(TAG, "Error loading Diabetes sessions: ${error.message}", error)
                emptyList()
            }
        )
    }

    override suspend fun getHtaSubSessions(sessionId: Long): List<SubSessionItem> {
        return contentRepository.getSubSessions(TreatmentType.HTA, sessionId).getOrDefault(emptyList())
            .map { subSessionContent ->
                val totalContent = subSessionContent.multimedia.size
                val readCount = dbHelper.getReadMultimediaCount(userId, TreatmentType.HTA.name, sessionId.toInt(), subSessionContent.id.toInt())
                SubSessionItem(
                    id = subSessionContent.id,
                    title = subSessionContent.title,
                    duration = subSessionContent.duration,
                    isCompleted = totalContent > 0 && readCount >= totalContent
                )
            }
    }

    override suspend fun getDiabetesSubSessions(sessionId: Long): List<SubSessionItem> {
        return contentRepository.getSubSessions(TreatmentType.DIABETES, sessionId).getOrDefault(emptyList())
            .map { subSessionContent ->
                val totalContent = subSessionContent.multimedia.size
                val readCount = dbHelper.getReadMultimediaCount(userId, TreatmentType.DIABETES.name, sessionId.toInt(), subSessionContent.id.toInt())
                SubSessionItem(
                    id = subSessionContent.id,
                    title = subSessionContent.title,
                    duration = subSessionContent.duration,
                    isCompleted = totalContent > 0 && readCount >= totalContent
                )
            }
    }
}