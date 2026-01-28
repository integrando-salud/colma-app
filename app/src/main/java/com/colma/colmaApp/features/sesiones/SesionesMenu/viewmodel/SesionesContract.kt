package com.colma.colmaApp.features.sesiones.SesionesMenu.viewmodel

import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SessionItem
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SubSessionItem

data class SesionesUiState(
    val htaSessions: List<SessionItem> = emptyList(),
    val diabetesSessions: List<SessionItem> = emptyList(),
    val subSessions: Map<Long, List<SubSessionItem>> = emptyMap(),
    val expandedSessionId: Long? = null,
    val isLoading: Boolean = true,
    val treatmentTypes: List<TreatmentType> = emptyList(),
    val error: String? = null
)

sealed class SesionesEvent {
    data class SetTreatmentTypes(val treatmentTypes: List<TreatmentType>) : SesionesEvent()
    data class ExpandSession(val sessionId: Long) : SesionesEvent()
    data class SubSessionClick(
        val parentSessionId: Long,
        val subSessionId: Long,
        val title: String,
        val treatmentType: TreatmentType
    ) : SesionesEvent()
    object BackClicked : SesionesEvent()
}
