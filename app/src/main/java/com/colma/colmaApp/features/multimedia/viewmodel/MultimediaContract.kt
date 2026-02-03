package com.colma.colmaApp.features.multimedia.viewmodel

import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption

/**
 * Represents the state of the Multimedia screen.
 */
data class MultimediaUiState(
    val multimediaOptions: List<MultimediaOption> = emptyList(),
    val currentMultimediaIndex: Int = -1,
    val isLoading: Boolean = true,
    val error: String? = null,
    val sessionTitle: String = "",
    val subSessionTitle: String = "",
) {
    val currentMultimediaOption: MultimediaOption?
        get() = multimediaOptions.getOrNull(currentMultimediaIndex)
}

/**
 * Defines the events that can be triggered from the UI.
 */
sealed class MultimediaEvent {
    object GoToNext : MultimediaEvent()
    object GoToPrevious : MultimediaEvent()
    object Finish : MultimediaEvent()
    data class LoadMultimedia(
        val sessionId: Long,
        val sessionTitle: String,
        val subSessionId: Long,
        val subSessionTitle: String,
        val treatmentTypesString: String,
        val startIndex: Int
    ) : MultimediaEvent()
}
