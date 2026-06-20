package com.colma.colmaApp.features.mood.viewmodel

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
        "Poco interés o placer en hacer las cosas.",
        "Sentirse deprimido, abatido o sin esperanza.",
        "Problemas para quedarse o permanecer dormido.",
        "Sentirse cansado o con poca energía.",
        "Tiene poco apetito o come mucho.",
        "Te sientes mal contigo mismo/a, o que eres un fracaso o que te has decepcionado a ti mismo/a o con tu familia.",
        "Problemas para concentrarte en actividades como leer el periódico o ver la televisión.",
        "¿Se mueve o habla tan despacio que otras personas podrían haberlo notado? ¿O está tan inquieto/a que se ha estado moviendo mucho más de lo habitual?"
    )

    private val optionLabels = listOf(
        "Nada", "Varios días", "La mayoría de días", "Casi todos los días"
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
        }
    }

    fun onEvent(event: MoodQuizEvent) {
        when (event) {
            is MoodQuizEvent.AnswerQuestion -> handleAnswerQuestion(event.index, event.answer)
            MoodQuizEvent.SaveClicked -> handleSave()
            MoodQuizEvent.ConfirmSubmit -> confirmSubmit()
            MoodQuizEvent.DismissConfirmDialog -> _uiState.update { it.copy(showConfirmDialog = false) }
            MoodQuizEvent.ConfirmMoreQuestions -> {
                _uiState.update { 
                    it.copy(
                        showMoreQuestionsDialog = false, 
                        showRestOfQuestions = true, 
                        firstTwoQuestionsLocked = true,
                        showCompletionDialog = true,
                        completionDialogMessage = "Continúe respondiendo las preguntas adicionales",
                        shouldNavigateAfterCompletion = false
                    )
                }
            }
            MoodQuizEvent.DismissMoreQuestionsDialog -> {
                _uiState.update { it.copy(showMoreQuestionsDialog = false) }
            }
            MoodQuizEvent.DismissWarningDialog -> _uiState.update { it.copy(showWarningDialog = false) }
            MoodQuizEvent.AcceptCompletionDialog -> handleAcceptCompletionDialog()
            MoodQuizEvent.DismissCompletionDialog -> _uiState.update { it.copy(showCompletionDialog = false) }
            MoodQuizEvent.BackPressed -> handleBack()
        }
    }

    private fun handleBack() {
        viewModelScope.launch {
            _sideEffects.send(SideEffect.NavigateBack)
        }
    }

    private fun handleAcceptCompletionDialog() {
        viewModelScope.launch {
            val shouldNavigate = _uiState.value.shouldNavigateAfterCompletion
            _uiState.update { it.copy(showCompletionDialog = false, shouldNavigateAfterCompletion = false) }
            if (shouldNavigate) {
                _sideEffects.send(SideEffect.NavigateBackWithResult)
            }
        }
    }

    private fun handleAnswerQuestion(index: Int, answer: Int) {
        val updatedAnswers = _uiState.value.answers.toMutableList()
        updatedAnswers[index] = answer
        _uiState.update { it.copy(answers = updatedAnswers) }
    }

    private fun handleSave() {
        viewModelScope.launch {
            val state = _uiState.value
            val firstTwoAnswered = state.answers[0] != -1 && state.answers[1] != -1

            if (!firstTwoAnswered) {
                _uiState.update { 
                    it.copy(
                        showWarningDialog = true,
                        warningDialogMessage = "Por favor responda las dos primeras preguntas"
                    )
                }
                return@launch
            }

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
                        _uiState.update { 
                            it.copy(
                                showWarningDialog = true,
                                warningDialogMessage = "Por favor responda todas las preguntas"
                            )
                        }
                    } else {
                        _uiState.update { it.copy(showConfirmDialog = true) }
                    }
                }
            }
        }
    }

    private fun confirmSubmit() {
        viewModelScope.launch {
            _uiState.update { it.copy(showConfirmDialog = false, isLoading = true, firstTwoQuestionsLocked = true) }
            val state = _uiState.value
            val firstTwoSum = state.answers.getOrElse(0) { 0 } + state.answers.getOrElse(1) { 0 }

            val answersToSave = if (firstTwoSum < 3) {
                state.answers.mapIndexedNotNull { index, i -> if(index <= 1 && i != -1) (index+1) to i else null }.toMap()
            } else {
                state.answers.mapIndexedNotNull { index, i -> if(i != -1) (index+1) to i else null }.toMap()
            }

            val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val result = repository.saveMoodRecord(answersToSave, isoFormatter.format(Date()))

            result.onSuccess { recordId ->
                _uiState.update { it.copy(isLoading = false) }
                
                // Attempt to sync individually
                viewModelScope.launch {
                    repository.syncMoodRecord(recordId)
                }

                val totalScore = answersToSave.values.filterNotNull().sum()
                val paisId = repository.getUserPaisId()
                
                val completionMessage = when {
                    totalScore >= 15 -> {
                        when (paisId) {
                            1 -> "Gracias por responder este cuestionario. Tus respuestas sugieren que podrías estar atravesando un momento difícil y que sería útil recibir apoyo.\n\nTe recomendamos pedir una cita de evaluación en tu centro de salud o servicio de salud mental lo antes posible.\n\nEn los próximos días, tu promotora de salud se va a comunicar con vos para hacer seguimiento: te va a preguntar si pudiste pedir la cita y, si hubo barreras, va a intentar ayudarte a resolverlas.\n\nEn caso que te estés sintiendo mal, podés acudir a una guardia o podés llamar al 107."
                            2 -> "Gracias por responder este cuestionario. Tus respuestas sugieren que podrías estar pasando por un momento difícil y que sería útil recibir apoyo.\n\nTe recomendamos sacar una cita con el servicio de psicología de tu centro de salud a la brevedad.\n\nEn los próximos días, tu promotora de salud se comunicará contigo para hacer seguimiento: te preguntará si pudiste solicitar la cita y, si hubo barreras, intentará ayudarte a resolverlas."
                            else -> "Registro guardado exitosamente"
                        }
                    }
                    totalScore in 10..14 -> {
                        when (paisId) {
                            1 -> "Tus respuestas sugieren que podés estar sintiendo un malestar emocional que podría afectar tu vida diaria. \n\nTe recomendamos consultar en tu centro de salud. \n\nMientras tanto, recordá que dormir bien, hacer actividad física y pasar tiempo con personas queridas puede ayudarte."
                            2 -> "Tus respuestas sugieren que puedes estar sintiendo un malestar emocional que podría afectar tu vida diaria. \n\nTe recomendamos consultar en tu centro de salud. \n\nMientras tanto, recuerda que dormir bien, hacer actividad física y pasar tiempo con personas queridas puede ayudarte."
                            else -> "Registro guardado exitosamente"
                        }
                    }
                    else -> "Registro guardado exitosamente"
                }

                _uiState.update { 
                    it.copy(
                        showCompletionDialog = true,
                        completionDialogMessage = completionMessage,
                        shouldNavigateAfterCompletion = true
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                _uiState.update { 
                    it.copy(
                        showWarningDialog = true,
                        warningDialogMessage = "Error guardando respuestas: ${error.message}"
                    )
                }
            }
        }
    }

    sealed class SideEffect {
        object NavigateBack : SideEffect()
        object NavigateBackWithResult : SideEffect()
    }
}
