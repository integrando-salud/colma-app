package com.colma.colmaApp.features.content.domain

import com.colma.colmaApp.features.content.model.MultimediaContent
import com.colma.colmaApp.features.content.model.SessionContent
import com.colma.colmaApp.features.content.model.SubSessionContent
import com.colma.colmaApp.features.content.model.TreatmentContent
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType

interface ContentRepository {
    suspend fun getAllTreatments(): Result<List<TreatmentContent>>
    suspend fun getSubSessions(treatmentType: TreatmentType, sessionId: Long): Result<List<SubSessionContent>>
    suspend fun getMultimediaOptions(treatmentType: TreatmentType, subSessionId: Long): Result<List<MultimediaContent>>
}
