package com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel

import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType

data class SessionStepsUiState(
    val sessionId: Long = -1L,
    val sessionTitle: String = "Sesión",
    val subSessionId: Long = -1L,
    val subSessionTitle: String = "",
    val treatmentTypes: List<TreatmentType> = emptyList(),
    val multimediaOptions: List<MultimediaOption> = emptyList(),
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class SessionStepsEvent {
    data class SessionStepClicked(val option: MultimediaOption, val index: Int) : SessionStepsEvent()
    object BackClicked : SessionStepsEvent()
}
