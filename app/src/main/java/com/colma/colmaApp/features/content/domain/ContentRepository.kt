package com.colma.colmaApp.features.content.domain

import com.colma.colmaApp.features.content.model.MultimediaContent
import com.colma.colmaApp.features.content.model.SessionContent
import com.colma.colmaApp.features.content.model.SubSessionContent
import com.colma.colmaApp.features.content.model.TreatmentContent
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType

data class SubSessionProgress(
    val currentIndex: Int,
    val totalCount: Int
)

interface ContentRepository {
    suspend fun getAllTreatments(): Result<List<TreatmentContent>>
    suspend fun getSubSessions(treatmentType: TreatmentType, sessionId: Long): Result<List<SubSessionContent>>
    suspend fun getMultimediaOptions(treatmentType: TreatmentType, sessionId: Long, subSessionId: Long): Result<List<MultimediaContent>>
    suspend fun getSubSessionProgress(treatmentType: TreatmentType, sessionId: Long, subSessionId: Long): Result<SubSessionProgress>
}
