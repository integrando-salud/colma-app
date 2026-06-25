package com.colma.colmaApp.features.sesiones.SessionSteps.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.SessionStepsRepository
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionStepsViewModel(private val repository: SessionStepsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionStepsUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun initialize(
        sessionId: Long,
        sessionTitle: String,
        subSessionId: Long,
        subSessionTitle: String,
        treatmentTypes: List<TreatmentType>
    ) {
        _uiState.update {
            it.copy(
                sessionId = sessionId,
                sessionTitle = sessionTitle,
                subSessionId = subSessionId,
                subSessionTitle = subSessionTitle,
                treatmentTypes = treatmentTypes,
            )
        }
        loadMultimediaOptions()
    }

    fun loadMultimediaOptions() {
        viewModelScope.launch {
            val showLoading = _uiState.value.multimediaOptions.isEmpty()
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            val state = _uiState.value

            val optionsResult = repository.getMultimediaOptions(state.treatmentTypes, state.sessionId, state.subSessionId)
            val progressResult = repository.getSubSessionProgress(state.treatmentTypes, state.sessionId, state.subSessionId)

            when {
                optionsResult.isSuccess && progressResult.isSuccess -> {
                    _uiState.update {
                        it.copy(
                            multimediaOptions = optionsResult.getOrThrow(),
                            currentStepIndex = progressResult.getOrThrow().currentIndex,
                            totalSteps = progressResult.getOrThrow().totalCount,
                            isLoading = false
                        )
                    }
                }
                optionsResult.isSuccess -> {
                    _uiState.update {
                        it.copy(
                            multimediaOptions = optionsResult.getOrThrow(),
                            isLoading = false
                        )
                    }
                }
                else -> {
                    val error = optionsResult.exceptionOrNull()?.message ?: progressResult.exceptionOrNull()?.message
                    _uiState.update { it.copy(error = error, isLoading = false) }
                }
            }
        }
    }

    fun onEvent(event: SessionStepsEvent) {
        when (event) {
            is SessionStepsEvent.SessionStepClicked -> {
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateToMultimedia(event.option, event.index))
                }
            }
            SessionStepsEvent.BackClicked -> {
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateBack)
                }
            }
        }
    }

    sealed class NavigationEvent {
        data class NavigateToMultimedia(val option: MultimediaOption, val index: Int) : NavigationEvent()
        object NavigateBack : NavigationEvent()
    }
}
