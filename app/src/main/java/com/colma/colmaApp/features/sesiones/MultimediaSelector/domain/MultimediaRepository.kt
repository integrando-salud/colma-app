package com.colma.colmaApp.features.sesiones.MultimediaSelector.domain

import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos

interface MultimediaRepository {
    suspend fun getMultimediaOptions(treatmentTypes: List<TreatmentType>, subSessionId: Long): Result<List<MultimediaOption>>
    suspend fun saveMultimediaRead(multimediaReadRecord: RegistrosSaludModelos.MultimediaReadRecord)
}
