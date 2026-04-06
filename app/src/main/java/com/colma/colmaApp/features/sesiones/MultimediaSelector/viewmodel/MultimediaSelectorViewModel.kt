package com.colma.colmaApp.features.sesiones.MultimediaSelector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaRepository
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MultimediaSelectorViewModel(private val repository: MultimediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MultimediaSelectorUiState())
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

    private fun loadMultimediaOptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val state = _uiState.value

            val optionsResult = repository.getMultimediaOptions(state.treatmentTypes, state.subSessionId)
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

    fun onEvent(event: MultimediaSelectorEvent) {
        when (event) {
            is MultimediaSelectorEvent.MultimediaOptionClicked -> {
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateToMultimedia(event.option, event.index))
                }
            }
            MultimediaSelectorEvent.BackClicked -> {
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
