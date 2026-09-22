package com.colma.colmaApp.features.multimedia.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.SessionStepsRepository
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaOption
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord
import com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class MultimediaViewModel(private val repository: SessionStepsRepository) : ViewModel() {

    private val TAG = "MultimediaViewModel"

    private val _uiState = MutableStateFlow(MultimediaUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun onEvent(event: MultimediaEvent) {
        when (event) {
            is MultimediaEvent.LoadMultimedia -> loadMultimedia(event)
            MultimediaEvent.GoToNext -> goToNext()
            MultimediaEvent.GoToPrevious -> goToPrevious()
            MultimediaEvent.Finish -> viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateBack) }
            is MultimediaEvent.QuizSubmitted -> saveQuizResponses(event.quizResponse, event.questionResponses)
        }
    }

    private fun loadMultimedia(event: MultimediaEvent.LoadMultimedia) {
        viewModelScope.launch {
            val treatmentTypes = event.treatmentTypesString.split(',').mapNotNull {
                try {
                    TreatmentType.valueOf(it.trim().uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    sessionTitle = event.sessionTitle,
                    subSessionTitle = event.subSessionTitle,
                    sessionId = event.sessionId,
                    subSessionId = event.subSessionId,
                    treatmentTypes = treatmentTypes
                )
            }

            if (treatmentTypes.isNotEmpty()) {
                repository.getMultimediaOptions(treatmentTypes, event.sessionId, event.subSessionId)
                    .onSuccess { options ->
                        _uiState.update {
                            it.copy(
                                multimediaOptions = options,
                                currentMultimediaIndex = event.startIndex,
                                isLoading = false
                            )
                        }
                        _uiState.value.currentMultimediaOption?.let { saveOptionReadRecord(it) }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                error = error.message ?: "Unknown error",
                                isLoading = false
                            )
                        }
                    }
            } else {
                _uiState.update {
                    it.copy(
                        error = "No valid treatment types provided.",
                        isLoading = false
                    )
                }
                viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateBack) } // Finish if no valid content
            }
        }
    }

    private fun goToNext() {
        val currentIndex = _uiState.value.currentMultimediaIndex
        val maxIndex = _uiState.value.multimediaOptions.lastIndex

        if (currentIndex < maxIndex) {
            _uiState.update { it.copy(currentMultimediaIndex = currentIndex + 1) }
            _uiState.value.currentMultimediaOption?.let { saveOptionReadRecord(it) }
        } else {
            viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateBack) }
        }
    }

    private fun goToPrevious() {
        val currentIndex = _uiState.value.currentMultimediaIndex
        if (currentIndex > 0) {
            _uiState.update { it.copy(currentMultimediaIndex = currentIndex - 1) }
            _uiState.value.currentMultimediaOption?.let { saveOptionReadRecord(it) }
        } else {
            viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateBack) }
        }
    }

    private fun saveOptionReadRecord(option: MultimediaOption) {
        // Do not auto-mark QUIZ or METAS as read on simple view/navigation
        if (option.type == MultimediaContentType.QUIZ || option.type == MultimediaContentType.METAS) {
            return
        }

        val userId = com.colma.colmaApp.servicios.session.SessionManager.getUserId() ?: 1
        val treatmentId = _uiState.value.treatmentTypes.firstOrNull()?.name ?: "UNKNOWN"
        val weekId = _uiState.value.sessionId
        val sessionId = _uiState.value.subSessionId
        val multimediaType = option.type.name
        val multimediaUniqueKey = generateMultimediaUniqueKey(
            option,
            treatmentId,
            weekId,
            sessionId
        )
        val record = RegistrosSaludModelos.MultimediaReadRecord(
            id_multimedia_read = 0,
            id_usuario = userId,
            treatment_id = treatmentId,
            week_id = weekId.toInt(),
            session_id = sessionId.toInt(),
            multimedia_type = multimediaType,
            multimedia_unique_key = multimediaUniqueKey,
            fecha_registro = null,
            fecha_sincronizacion = null,
            tratamiento_multimedia_id = option.elementoId,
            tratamiento_elemento_id = option.elementoId
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMultimediaRead(record)
        }
    }

    private fun saveQuizResponses(quizResponse: QuizResponseRecord, questionResponses: List<QuestionResponseRecord>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQuizResponses(quizResponse, questionResponses)
                .onSuccess { quizResponseId ->
                    Log.d(TAG, "Quiz responses saved successfully with ID: $quizResponseId")

                    // Mark Quiz as read in multimedia_read_table upon submission
                    val userId = quizResponse.id_usuario
                    val treatmentId = _uiState.value.treatmentTypes.firstOrNull()?.name ?: "UNKNOWN"
                    val weekId = quizResponse.week_id ?: _uiState.value.sessionId.toInt()
                    val sessionId = quizResponse.session_id ?: _uiState.value.subSessionId.toInt()
                    val multimediaUniqueKey = quizResponse.multimedia_unique_key ?: "QUIZ_UNKNOWN"
                    val elemId = quizResponse.tratamiento_elemento_id ?: _uiState.value.currentMultimediaOption?.elementoId
                    val readRecord = RegistrosSaludModelos.MultimediaReadRecord(
                        id_multimedia_read = 0,
                        id_usuario = userId,
                        treatment_id = treatmentId,
                        week_id = weekId,
                        session_id = sessionId,
                        multimedia_type = MultimediaContentType.QUIZ.name,
                        multimedia_unique_key = multimediaUniqueKey,
                        fecha_registro = null,
                        fecha_sincronizacion = null,
                        tratamiento_multimedia_id = elemId,
                        tratamiento_elemento_id = elemId
                    )
                    repository.saveMultimediaRead(readRecord)


                    // Attempt to sync with server
                    viewModelScope.launch {
                        repository.syncQuizResponse(quizResponse, questionResponses)
                            .onSuccess { message ->
                                Log.d(TAG, "Quiz sync successful: $message")
                            }
                            .onFailure { error ->
                                Log.w(TAG, "Quiz sync failed (will retry later): ${error.message}")
                            }
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Error saving quiz responses: ${error.message}")
                }
        }
    }

    private fun generateMultimediaUniqueKey(
        option: MultimediaOption,
        treatmentId: String,
        weekId: Long,
        sessionId: Long
    ): String {
        return com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaKeyUtils.generateMultimediaUniqueKey(
            option = option,
            treatmentId = treatmentId,
            weekId = weekId,
            sessionId = sessionId
        )
    }

    sealed class NavigationEvent {
        object NavigateBack : NavigationEvent()
    }
}