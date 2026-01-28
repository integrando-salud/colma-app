package com.colma.colmaApp.features.sesiones.MultimediaSelector.domain

interface MultimediaRepository {
    suspend fun getMultimediaOptions(treatmentTypes: List<TreatmentType>, subSessionId: Long): Result<List<MultimediaOption>>
}
