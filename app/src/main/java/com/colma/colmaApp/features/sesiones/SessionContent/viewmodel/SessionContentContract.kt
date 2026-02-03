package com.colma.colmaApp.features.sesiones.SessionContent.viewmodel

data class SessionContentUiState(
    val sessionId: Long = -1,
    val sessionTitle: String = "Sesión",
    val subSessionId: Long? = null,
    val subSessionTitle: String? = null,
    val selectedTab: Int = 0,
    val informationContent: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class SessionContentEvent {
    data class LoadSession(
        val sessionId: Long,
        val sessionTitle: String,
        val subSessionId: Long?,
        val subSessionTitle: String?
    ) : SessionContentEvent()
    data class TabSelected(val index: Int) : SessionContentEvent()
    object BackClicked : SessionContentEvent()
    object TerminarClicked : SessionContentEvent()
}
