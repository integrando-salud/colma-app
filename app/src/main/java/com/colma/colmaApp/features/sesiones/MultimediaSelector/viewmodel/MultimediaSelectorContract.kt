package com.colma.colmaApp.features.sesiones.MultimediaSelector.viewmodel

import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType

data class MultimediaSelectorUiState(
    val sessionId: Long = -1L,
    val sessionTitle: String = "Sesión",
    val subSessionId: Long = -1L,
    val subSessionTitle: String = "",
    val treatmentTypes: List<TreatmentType> = emptyList(),
    val multimediaOptions: List<MultimediaOption> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class MultimediaSelectorEvent {
    data class MultimediaOptionClicked(val option: MultimediaOption, val index: Int) : MultimediaSelectorEvent()
    object BackClicked : MultimediaSelectorEvent()
}
