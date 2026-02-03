    package com.colma.colmaApp.features.multimediaScreens

import android.app.Activity
import android.content.Intent
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.data.MultimediaRepositoryImpl
import kotlinx.coroutines.runBlocking // Using runBlocking for simplicity in a top-level function, ideally this would be handled differently
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


fun navigateToNextOrFinish(
    activity: Activity,
    sessionId: Long,
    sessionTitle: String,
    subSessionId: Long,
    subSessionTitle: String,
    treatmentTypesString: String,
    currentMultimediaIndex: Int
) {
    val treatmentTypes = treatmentTypesString.split(',').mapNotNull {
        try {
            TreatmentType.valueOf(it.trim().uppercase())
        } catch (e: IllegalArgumentException) { null }
    }

    if (treatmentTypes.isEmpty()) {
        activity.finish()
        return
    }

    val multimediaRepository = MultimediaRepositoryImpl()
    val options: List<MultimediaOption> = runBlocking(Dispatchers.IO) { // Use runBlocking for synchronous call in this context
        multimediaRepository.getMultimediaOptions(treatmentTypes, subSessionId).getOrNull() ?: emptyList()
    }
    val nextIndex = currentMultimediaIndex + 1

    if (nextIndex < options.size) {
        val nextOption = options[nextIndex]
        val intent = when (nextOption.type) {
            MultimediaContentType.DESCRIPTION -> Intent(activity, InformationDisplayActivity::class.java)
            MultimediaContentType.VIDEO -> Intent(activity, InstructionalVideoActivity::class.java)
            MultimediaContentType.AUDIO -> Intent(activity, ExplanatoryAudioActivity::class.java)
            MultimediaContentType.METAS -> Intent(activity, MetasActivity::class.java)
            MultimediaContentType.QUIZ -> Intent(activity, QuizActivity::class.java)
        }

        intent.apply {
            putExtra("sessionId", sessionId)
            putExtra("sessionTitle", sessionTitle)
            putExtra("subSessionId", subSessionId)
            putExtra("subSessionTitle", subSessionTitle)
            putExtra("treatmentTypes", treatmentTypesString)
            putExtra("currentMultimediaIndex", nextIndex)

            if (nextOption.contentAssetId != null) {
                putExtra("contentAssetId", nextOption.contentAssetId)
            }
            if (nextOption.quizId != null) {
                putExtra("quizId", nextOption.quizId)
            }
            if (nextOption.metasId != null) {
                putExtra("metasType", nextOption.metasId)
            }
            if (nextOption.type == MultimediaContentType.DESCRIPTION) {
                putExtra("displayContent", nextOption.fullDescription ?: nextOption.description)
            }
        }
        activity.startActivity(intent)
    }
    activity.finish()
}
