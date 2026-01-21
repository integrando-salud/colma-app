package com.colma.colmaApp.features.multimediaScreens

import android.app.Activity
import android.content.Intent
import com.colma.colmaApp.features.sesiones.MultimediaSelector.MultimediaContentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.TreatmentType
import com.colma.colmaApp.features.sesiones.MultimediaSelector.getMultimediaOptions

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

    val options = treatmentTypes.flatMap { getMultimediaOptions(it, subSessionId) }
    val nextIndex = currentMultimediaIndex + 1

    if (nextIndex < options.size) {
        val nextOption = options[nextIndex]
        val intent = when (nextOption.type) {
            MultimediaContentType.DESCRIPTION -> Intent(activity, WelcomeActivity::class.java)
            MultimediaContentType.VIDEO -> Intent(activity, ActionActivity::class.java)
            MultimediaContentType.AUDIO -> Intent(activity, FinishActivity::class.java)
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
