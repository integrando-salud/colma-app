package com.colma.colmaApp.features.sesiones.SessionList.data

import android.content.Context
import android.util.Log
import com.colma.colmaApp.features.content.domain.ContentRepository
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SessionList.domain.SessionListRepository
import com.colma.colmaApp.features.sesiones.SessionList.domain.SessionItem
import com.colma.colmaApp.features.sesiones.SessionList.domain.SubSessionItem
import com.colma.colmaApp.utils.ResourceUtils
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper
import com.colma.colmaApp.servicios.session.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionListRepositoryImpl(
    private val contentRepository: ContentRepository,
    private val context: Context,
    private val dbHelper: DbHelper
) : SessionListRepository {
    private val TAG = "SessionListRepository"
    private val userId: Int

    init {
        SessionManager.init(context.applicationContext)
        userId = SessionManager.getUserId() ?: 1
    }

    private fun getElapsedWeeks(fechaRegistro: String?): Int {
        if (fechaRegistro == null) return 0
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            val regDate = formatter.parse(fechaRegistro)
            val diff = Date().time - (regDate?.time ?: Date().time)
            val days = diff / (1000 * 60 * 60 * 24)
            (days / 7).toInt()
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun getHtaSessions(): List<SessionItem> = withContext(Dispatchers.IO) {
        val fechaRegistro = dbHelper.getUsuarioPrimerLogin(userId)
        val elapsedWeeks = getElapsedWeeks(fechaRegistro)

        contentRepository.getAllTreatments().fold(
            onSuccess = { treatmentContents ->
                Log.d(TAG, "Successfully loaded ${treatmentContents.size} treatments")
                treatmentContents
                    .filter { it.id == TreatmentType.HTA }
                    .flatMap { it.sessions }
                    // .filterIndexed { index, _ -> index <= elapsedWeeks }
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

    override suspend fun getDiabetesSessions(): List<SessionItem> = withContext(Dispatchers.IO) {
        val fechaRegistro = dbHelper.getUsuarioPrimerLogin(userId)
        val elapsedWeeks = getElapsedWeeks(fechaRegistro)

        contentRepository.getAllTreatments().fold(
            onSuccess = { treatmentContents ->
                Log.d(TAG, "Successfully loaded ${treatmentContents.size} treatments")
                treatmentContents
                    .filter { it.id == TreatmentType.DIABETES }
                    .flatMap { it.sessions }
                    // .filterIndexed { index, _ -> index <= elapsedWeeks }
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

    override suspend fun getDbtHtaSessions(): List<SessionItem> = withContext(Dispatchers.IO) {
        val fechaRegistro = dbHelper.getUsuarioPrimerLogin(userId)
        val elapsedWeeks = getElapsedWeeks(fechaRegistro)

        contentRepository.getAllTreatments().fold(
            onSuccess = { treatmentContents ->
                Log.d(TAG, "Successfully loaded ${treatmentContents.size} treatments")
                treatmentContents
                    .filter { it.id == TreatmentType.DBT_HTA }
                    .flatMap { it.sessions }
                    // .filterIndexed { index, _ -> index <= elapsedWeeks }
                    .map { sessionContent ->
                        SessionItem(
                            id = sessionContent.id,
                            title = sessionContent.title,
                            treatmentType = TreatmentType.DBT_HTA,
                            thumbnailRes = com.colma.colmaApp.utils.ResourceUtils.getResourceId(context, sessionContent.thumbnail, "drawable"),
                            duration = sessionContent.duration
                        )
                    }
            },
            onFailure = { error ->
                Log.e(TAG, "Error loading DBT_HTA sessions: ${error.message}", error)
                emptyList()
            }
        )
    }

    override suspend fun getDbtHtaSubSessions(sessionId: Long): List<SubSessionItem> = withContext(Dispatchers.IO) {
        contentRepository.getSubSessions(TreatmentType.DBT_HTA, sessionId).getOrDefault(emptyList())
            .map { subSessionContent ->
                val totalContent = subSessionContent.multimedia.size
                val readCount = dbHelper.getReadMultimediaCount(userId, TreatmentType.DBT_HTA.name, sessionId.toInt(), subSessionContent.id.toInt())
                SubSessionItem(
                    id = subSessionContent.id,
                    title = subSessionContent.title,
                    duration = subSessionContent.duration,
                    isCompleted = totalContent > 0 && readCount >= totalContent
                )
            }
    }

    override suspend fun debugAdvanceOneWeek() {
        dbHelper.debugSubtractWeekFromRegistrationDate(userId)
    }

    override suspend fun getHtaSubSessions(sessionId: Long): List<SubSessionItem> = withContext(Dispatchers.IO) {
        contentRepository.getSubSessions(TreatmentType.HTA, sessionId).getOrDefault(emptyList())
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

    override suspend fun getDiabetesSubSessions(sessionId: Long): List<SubSessionItem> = withContext(Dispatchers.IO) {
        contentRepository.getSubSessions(TreatmentType.DIABETES, sessionId).getOrDefault(emptyList())
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