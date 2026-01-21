package com.colma.colmaApp.features.sesiones.SessionContent.data

import com.colma.colmaApp.features.sesiones.SessionContent.domain.SessionContentRepository
import com.colma.colmaApp.features.sesiones.SessionContent.domain.SessionDisplayContent
import kotlinx.coroutines.delay

class SessionContentRepositoryImpl : SessionContentRepository {
    override suspend fun getSessionContent(sessionId: Long, subSessionId: Long?): Result<SessionDisplayContent> {
        delay(300) // simulate loading
        return Result.success(SessionDisplayContent("No custom information available for this session."))
    }
}
