package com.colma.colmaApp.features.sesiones.SessionContent.viewmodel

data class SessionContentUiState(
    val sessionId: Long = -1,
    val sessionTitle: String = "Sesión",
    val subSessionId: Long? = null,
    val subSessionTitle: String? = null,
    val selectedTab: Int = 0,
    val informationContent: String = "",
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val durationMs: Long = 0L,
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
    object PlayPauseToggle : SessionContentEvent()
    data class SeekAudio(val position: Long) : SessionContentEvent()
    data class UpdatePlaybackPosition(val position: Long) : SessionContentEvent()
    data class PlayerReady(val duration: Long) : SessionContentEvent()
    object BackClicked : SessionContentEvent()
    object TerminarClicked : SessionContentEvent()
}
