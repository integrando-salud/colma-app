package com.colma.colmaApp.features.sesiones.SesionesMenu.domain

import com.colma.colmaApp.R
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType

data class SessionItem(
    val id: Long,
    val title: String,
    val treatmentType: TreatmentType,
    val thumbnailRes: Int = R.drawable.missing_photo,
    val duration: String = "10 min"
)

data class SubSessionItem(
    val id: Long,
    val title: String,
    val duration: String = "5 min",
    val isCompleted: Boolean = false
)

interface SesionesRepository {
    suspend fun getHtaSessions(): List<SessionItem>
    suspend fun getDiabetesSessions(): List<SessionItem>
    suspend fun getHtaSubSessions(sessionId: Long): List<SubSessionItem>
    suspend fun getDiabetesSubSessions(sessionId: Long): List<SubSessionItem>
}
