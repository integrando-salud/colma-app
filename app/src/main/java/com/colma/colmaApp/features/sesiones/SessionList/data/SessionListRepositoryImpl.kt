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
    companion object {
        private const val PREFS_NAME = "session_testing_prefs"
        private const val KEY_IS_TESTING_MODE = "is_testing_mode"
        private const val KEY_TEST_UNLOCKED_WEEKS = "testing_unlocked_weeks"
    }

    private val TAG = "SessionListRepository"
    private val userId: Int

    private val prefs: android.content.SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

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

    private fun getUnlockedWeeksCount(): Int {
        val isTest = prefs.getBoolean(KEY_IS_TESTING_MODE, false)
        return if (isTest) {
            prefs.getInt(KEY_TEST_UNLOCKED_WEEKS, 1)
        } else {
            val fechaRegistro = dbHelper.getUsuarioPrimerLogin(userId)
            getElapsedWeeks(fechaRegistro) + 1
        }
    }

    override suspend fun getHtaSessions(): List<SessionItem> = withContext(Dispatchers.IO) {
        val unlockedWeeks = getUnlockedWeeksCount()

        contentRepository.getAllTreatments().fold(
            onSuccess = { treatmentContents ->
                Log.d(TAG, "Successfully loaded ${treatmentContents.size} treatments")
                treatmentContents
                    .filter { it.id == TreatmentType.HTA }
                    .flatMap { it.sessions }
                    .filterIndexed { index, _ -> index < unlockedWeeks }
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
        val unlockedWeeks = getUnlockedWeeksCount()

        contentRepository.getAllTreatments().fold(
            onSuccess = { treatmentContents ->
                Log.d(TAG, "Successfully loaded ${treatmentContents.size} treatments")
                treatmentContents
                    .filter { it.id == TreatmentType.DIABETES }
                    .flatMap { it.sessions }
                    .filterIndexed { index, _ -> index < unlockedWeeks }
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
        val unlockedWeeks = getUnlockedWeeksCount()

        contentRepository.getAllTreatments().fold(
            onSuccess = { treatmentContents ->
                Log.d(TAG, "Successfully loaded ${treatmentContents.size} treatments")
                treatmentContents
                    .filter { it.id == TreatmentType.DBT_HTA }
                    .flatMap { it.sessions }
                    .filterIndexed { index, _ -> index < unlockedWeeks }
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

    private fun isSubSessionCompleted(
        treatmentType: TreatmentType,
        sessionId: Long,
        subSessionId: Long,
        multimediaList: List<com.colma.colmaApp.features.content.model.MultimediaContent>
    ): Boolean {
        if (multimediaList.isEmpty()) return false

        val processedContents = if (treatmentType == TreatmentType.DBT_HTA) {
            val hasMetas = multimediaList.any { it.type == com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType.METAS }
            if (hasMetas) {
                val metasTemplate = multimediaList.first { it.type == com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType.METAS }
                val unifiedMetas = metasTemplate.copy(
                    title = "Metas (Diabetes e Hipertensión)",
                    metasId = "dbt_hta"
                )
                val resultList = mutableListOf<com.colma.colmaApp.features.content.model.MultimediaContent>()
                var addedUnified = false
                for (item in multimediaList) {
                    if (item.type == com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType.METAS) {
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
                multimediaList
            }
        } else {
            multimediaList
        }

        val size = processedContents.size
        return processedContents.all { multimediaContent ->
            val isLocalFile = multimediaContent.contentAsset?.contains("/") == true || multimediaContent.contentAsset?.contains("\\") == true
            val localFilePath = if (isLocalFile) multimediaContent.contentAsset else null
            val contentAssetId = if (isLocalFile) null else multimediaContent.contentAsset?.let {
                ResourceUtils.getResourceId(context, it, "raw")
            }
            val originalTitle = multimediaContent.title.trim()
            val finalTitle = if (originalTitle.isNotEmpty()) {
                originalTitle
            } else if (multimediaContent.type == com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType.DESCRIPTION) {
                if (processedContents.indexOf(multimediaContent) == 0) {
                    "Bienvenida"
                } else if (processedContents.indexOf(multimediaContent) == size - 1 && size > 1) {
                    "Fin de la sesión"
                } else {
                    "Descripción"
                }
            } else {
                multimediaContent.type.name
            }

            val uniqueKey = com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaKeyUtils.generateMultimediaUniqueKey(
                type = multimediaContent.type,
                title = finalTitle,
                contentAssetId = contentAssetId,
                metasId = multimediaContent.metasId,
                treatmentId = treatmentType.name,
                weekId = sessionId,
                sessionId = subSessionId,
                localFilePath = localFilePath
            )

            dbHelper.getMultimediaReadRecordExists(
                idUsuario = userId,
                treatmentId = treatmentType.name,
                weekId = sessionId.toInt(),
                sessionId = subSessionId.toInt(),
                multimediaType = multimediaContent.type.name,
                multimediaUniqueKey = uniqueKey
            )
        }
    }

    override suspend fun getDbtHtaSubSessions(sessionId: Long): List<SubSessionItem> = withContext(Dispatchers.IO) {
        contentRepository.getSubSessions(TreatmentType.DBT_HTA, sessionId).getOrDefault(emptyList())
            .map { subSessionContent ->
                SubSessionItem(
                    id = subSessionContent.id,
                    title = subSessionContent.title,
                    duration = subSessionContent.duration,
                    isCompleted = isSubSessionCompleted(
                        treatmentType = TreatmentType.DBT_HTA,
                        sessionId = sessionId,
                        subSessionId = subSessionContent.id,
                        multimediaList = subSessionContent.multimedia
                    )
                )
            }
    }

    override suspend fun debugAdvanceOneWeek() {
        dbHelper.debugSubtractWeekFromRegistrationDate(userId)
    }

    override suspend fun getHtaSubSessions(sessionId: Long): List<SubSessionItem> = withContext(Dispatchers.IO) {
        contentRepository.getSubSessions(TreatmentType.HTA, sessionId).getOrDefault(emptyList())
            .map { subSessionContent ->
                SubSessionItem(
                    id = subSessionContent.id,
                    title = subSessionContent.title,
                    duration = subSessionContent.duration,
                    isCompleted = isSubSessionCompleted(
                        treatmentType = TreatmentType.HTA,
                        sessionId = sessionId,
                        subSessionId = subSessionContent.id,
                        multimediaList = subSessionContent.multimedia
                    )
                )
            }
    }

    override suspend fun getDiabetesSubSessions(sessionId: Long): List<SubSessionItem> = withContext(Dispatchers.IO) {
        contentRepository.getSubSessions(TreatmentType.DIABETES, sessionId).getOrDefault(emptyList())
            .map { subSessionContent ->
                SubSessionItem(
                    id = subSessionContent.id,
                    title = subSessionContent.title,
                    duration = subSessionContent.duration,
                    isCompleted = isSubSessionCompleted(
                        treatmentType = TreatmentType.DIABETES,
                        sessionId = sessionId,
                        subSessionId = subSessionContent.id,
                        multimediaList = subSessionContent.multimedia
                    )
                )
            }
    }

    override suspend fun isTestingMode(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(KEY_IS_TESTING_MODE, false)
    }

    override suspend fun getTestingUnlockedWeeks(): Int = withContext(Dispatchers.IO) {
        prefs.getInt(KEY_TEST_UNLOCKED_WEEKS, 1)
    }

    override suspend fun toggleTestingMode(): Boolean = withContext(Dispatchers.IO) {
        val current = prefs.getBoolean(KEY_IS_TESTING_MODE, false)
        val newMode = !current
        if (newMode) {
            prefs.edit()
                .putBoolean(KEY_IS_TESTING_MODE, true)
                .putInt(KEY_TEST_UNLOCKED_WEEKS, 1)
                .apply()
        } else {
            com.colma.colmaApp.utils.notifications.NotificationHelper.cancelWeeklyTestUnlock(context)
            prefs.edit()
                .putBoolean(KEY_IS_TESTING_MODE, false)
                .putInt(KEY_TEST_UNLOCKED_WEEKS, 1)
                .apply()
        }
        newMode
    }

    override suspend fun unlockNextWeek(): Int? = withContext(Dispatchers.IO) {
        val isTest = prefs.getBoolean(KEY_IS_TESTING_MODE, false)
        if (!isTest) {
            prefs.edit().putBoolean(KEY_IS_TESTING_MODE, true).apply()
        }
        val currentUnlocked = prefs.getInt(KEY_TEST_UNLOCKED_WEEKS, 1)
        val nextWeek = currentUnlocked + 1
        if (nextWeek > 16) return@withContext null

        prefs.edit()
            .putInt(KEY_TEST_UNLOCKED_WEEKS, nextWeek)
            .apply()
        Log.d(TAG, "Unlocked test week: $nextWeek")
        nextWeek
    }

    override suspend fun scheduleNextWeekInTenMinutes(): Pair<Int, Long>? = withContext(Dispatchers.IO) {
        val nextWeek = unlockNextWeek() ?: return@withContext null
        Pair(nextWeek, System.currentTimeMillis())
    }
}