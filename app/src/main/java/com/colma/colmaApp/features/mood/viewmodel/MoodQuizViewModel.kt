package com.colma.colmaApp.features.mood.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.mood.domain.MoodRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodQuizViewModel(private val repository: MoodRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MoodQuizUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffects = Channel<SideEffect>()
    val sideEffects = _sideEffects.receiveAsFlow()
    
    private val allQuestions = listOf(
        "Poco interés o placer en hacer las cosas",
        "Sentirse deprimido, abatido o sin esperanza",
        "Problemas para quedarse o permanecer dormido",
        "Sentirse cansado o con poca energía",
        "Poco apetito o comer mucho?",
        "Te sientes mal sobre ti mismo -- o que eres un fracaso o has decepcionado a ti mismo o tu familia",
        "Problemas para concentrarte en cosas, como leer el periodico o ver la television",
        "¿Se mueve o habla tan despacio que otras personas podrían haberlo notado? ¿O está tan inquieto que se ha estado moviendo mucho más de lo habitual?"
    )

    private val optionLabels = listOf(
        "Nada", "Varios dias", "la mayoria de dias", "Casi todos los dias"
    )

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    questions = allQuestions,
                    optionLabels = optionLabels,
                    answers = List(allQuestions.size) { -1 }
                )
            }
            repository.createDraftMoodRecord().onSuccess { id ->
                _uiState.update { it.copy(draftRecordId = id) }
            }.onFailure {
                _sideEffects.send(SideEffect.ShowToast("Error creando el borrador", Toast.LENGTH_LONG))
            }
        }
    }

    fun onEvent(event: MoodQuizEvent) {
        when (event) {
            is MoodQuizEvent.AnswerQuestion -> handleAnswerQuestion(event.index, event.answer)
            MoodQuizEvent.SaveClicked -> handleSave()
            MoodQuizEvent.ConfirmSubmit -> confirmSubmit()
            MoodQuizEvent.DismissConfirmDialog -> _uiState.update { it.copy(showConfirmDialog = false) }
            MoodQuizEvent.ConfirmMoreQuestions -> {
                _uiState.update { it.copy(showMoreQuestionsDialog = false, showRestOfQuestions = true) }
                viewModelScope.launch {
                    _sideEffects.send(SideEffect.ShowToast("Contesta las preguntas adicionales"))
                }
            }
            MoodQuizEvent.DismissMoreQuestionsDialog -> {
                _uiState.update { it.copy(showMoreQuestionsDialog = false, firstTwoQuestionsLocked = false) }
            }
            MoodQuizEvent.UnlockFirstTwoQuestions -> _uiState.update { it.copy(firstTwoQuestionsLocked = false) }
            MoodQuizEvent.BackPressed -> handleBack()
        }
    }
    
    private fun handleBack() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.answers.all { it == -1 } && state.draftRecordId != null) {
                repository.deleteMoodRecord(state.draftRecordId)
            }
            _sideEffects.send(SideEffect.NavigateBack)
        }
    }

    private fun handleAnswerQuestion(index: Int, answer: Int) {
        val updatedAnswers = _uiState.value.answers.toMutableList()
        updatedAnswers[index] = answer
        _uiState.update { it.copy(answers = updatedAnswers) }

        if (index <= 1) { // only update DB for first two questions for now
            val state = _uiState.value
            if (state.draftRecordId != null) {
                viewModelScope.launch {
                    val answersMap = state.answers.mapIndexedNotNull { i, ans ->
                        if (ans != -1) (i + 1) to ans else null
                    }.toMap()
                    val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    repository.updateMoodRecord(state.draftRecordId, answersMap, isoFormatter.format(Date()))
                }
            }
        }
    }
    
    private fun handleSave() {
        viewModelScope.launch {
            val state = _uiState.value
            val firstTwoAnswered = state.answers[0] != -1 && state.answers[1] != -1

            if (!firstTwoAnswered) {
                _sideEffects.send(SideEffect.ShowToast("Por favor responda las dos primeras preguntas"))
                return@launch
            }

            _uiState.update { it.copy(firstTwoQuestionsLocked = true) }
            val firstTwoSum = state.answers[0] + state.answers[1]

            if (firstTwoSum < 3) {
                // Submit only first 2
                _uiState.update { it.copy(showConfirmDialog = true) }
            } else {
                if (!state.showRestOfQuestions) {
                    _uiState.update { it.copy(showMoreQuestionsDialog = true) }
                } else {
                    // check if all are answered
                    if (state.answers.any { it == -1 }) {
                        _sideEffects.send(SideEffect.ShowToast("Por favor responda todas las preguntas"))
                    } else {
                        _uiState.update { it.copy(showConfirmDialog = true) }
                    }
                }
            }
        }
    }

    private fun confirmSubmit() {
        viewModelScope.launch {
            _uiState.update { it.copy(showConfirmDialog = false, isLoading = true) }
            val state = _uiState.value
            val firstTwoSum = state.answers.getOrElse(0) { 0 } + state.answers.getOrElse(1) { 0 }

            val answersToSave = if (firstTwoSum < 3) {
                state.answers.mapIndexedNotNull { index, i -> if(index <= 1 && i != -1) (index+1) to i else null }.toMap()
            } else {
                state.answers.mapIndexedNotNull { index, i -> if(i != -1) (index+1) to i else null }.toMap()
            }
            
            if (state.draftRecordId != null) {
                val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                repository.updateMoodRecord(state.draftRecordId, answersToSave, isoFormatter.format(Date()))
                _uiState.update { it.copy(isLoading = false) }
                _sideEffects.send(SideEffect.ShowToast("Respuestas guardadas"))
                _sideEffects.send(SideEffect.NavigateBackWithResult)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _sideEffects.send(SideEffect.ShowToast("Error: No se encontró el borrador"))
            }
        }
    }

    sealed class SideEffect {
        data class ShowToast(val message: String, val duration: Int = Toast.LENGTH_SHORT) : SideEffect()
        object NavigateBack : SideEffect()
        object NavigateBackWithResult : SideEffect()
    }
}
