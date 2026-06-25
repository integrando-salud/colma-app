package com.colma.colmaApp.features.sesiones.SessionList.viewmodel

import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SessionList.domain.SessionItem
import com.colma.colmaApp.features.sesiones.SessionList.domain.SubSessionItem

data class SessionListUiState(
    val htaSessions: List<SessionItem> = emptyList(),
    val diabetesSessions: List<SessionItem> = emptyList(),
    val subSessions: Map<Long, List<SubSessionItem>> = emptyMap(),
    val expandedSessionId: Long? = null,
    val isLoading: Boolean = true,
    val treatmentTypes: List<TreatmentType> = emptyList(),
    val error: String? = null
)

sealed class SessionListEvent {
    data class SetTreatmentTypes(val treatmentTypes: List<TreatmentType>) : SessionListEvent()
    data class ExpandSession(val sessionId: Long) : SessionListEvent()
    data class SubSessionClick(
        val parentSessionId: Long,
        val subSessionId: Long,
        val title: String,
        val treatmentType: TreatmentType
    ) : SessionListEvent()
    object BackClicked : SessionListEvent()
    object DebugAdvanceWeek : SessionListEvent()
    object Refresh : SessionListEvent()
}
