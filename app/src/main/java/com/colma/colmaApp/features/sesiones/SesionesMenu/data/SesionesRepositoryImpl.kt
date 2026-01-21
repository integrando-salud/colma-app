package com.colma.colmaApp.features.sesiones.SesionesMenu.data

import com.colma.colmaApp.R
import com.colma.colmaApp.features.sesiones.MultimediaSelector.TreatmentType
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SesionesRepository
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SessionItem
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SubSessionItem
import kotlinx.coroutines.delay

class SesionesRepositoryImpl : SesionesRepository {
    override suspend fun getHtaSessions(): List<SessionItem> {
        delay(500) // Simulate network/db call
        return (1..16).map {
            SessionItem(it.toLong(), "Semana $it hipertensión", TreatmentType.HTA, thumbnailRes = R.drawable.missing_photo)
        }
    }

    override suspend fun getDiabetesSessions(): List<SessionItem> {
        delay(500) // Simulate network/db call
        return (1..16).map {
            SessionItem(it.toLong() + 16, "Semana $it Diabetes", TreatmentType.DIABETES, thumbnailRes = R.drawable.missing_photo)
        }
    }

    override suspend fun getHtaSubSessions(sessionId: Long): List<SubSessionItem> {
        delay(200)
        return listOf(
            SubSessionItem(1, "Sesion 1", "05:00"),
            SubSessionItem(2, "Sesion 2", "07:00"),
            SubSessionItem(3, "Sesion 3", "03:00"),
        )
    }

    override suspend fun getDiabetesSubSessions(sessionId: Long): List<SubSessionItem> {
        delay(200)
        return listOf(
            SubSessionItem(1, "Sesion 1", "05:00"),
            SubSessionItem(2, "Sesion 2", "07:00"),
            SubSessionItem(3, "Sesion 3", "03:00"),
        )
    }
}
