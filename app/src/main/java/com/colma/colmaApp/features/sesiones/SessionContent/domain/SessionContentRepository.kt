package com.colma.colmaApp.features.sesiones.SessionContent.domain

data class SessionDisplayContent(val information: String)

interface SessionContentRepository {
    suspend fun getSessionContent(sessionId: Long, subSessionId: Long?): Result<SessionDisplayContent>
}
