package com.colma.colmaApp.features.sesiones.MultimediaSelector.domain

import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord
import com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord

interface MultimediaRepository {
    suspend fun getMultimediaOptions(treatmentTypes: List<TreatmentType>, subSessionId: Long): Result<List<MultimediaOption>>
    suspend fun saveMultimediaRead(multimediaReadRecord: RegistrosSaludModelos.MultimediaReadRecord)
    suspend fun saveQuizResponses(quizResponse: QuizResponseRecord, questionResponses: List<QuestionResponseRecord>)
}
