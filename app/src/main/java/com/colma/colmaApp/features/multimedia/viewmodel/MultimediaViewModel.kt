package com.colma.colmaApp.features.multimedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaRepository
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MultimediaViewModel(private val repository: MultimediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MultimediaUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun onEvent(event: MultimediaEvent) {
        when (event) {
            is MultimediaEvent.LoadMultimedia -> loadMultimedia(event)
            MultimediaEvent.GoToNext -> goToNext()
            MultimediaEvent.GoToPrevious -> goToPrevious()
            MultimediaEvent.Finish -> finish()
        }
    }

    private fun loadMultimedia(event: MultimediaEvent.LoadMultimedia) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    sessionTitle = event.sessionTitle,
                    subSessionTitle = event.subSessionTitle
                )
            }

            val treatmentTypes = event.treatmentTypesString.split(',').mapNotNull {
                try {
                    TreatmentType.valueOf(it.trim().uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

            if (treatmentTypes.isNotEmpty()) {
                repository.getMultimediaOptions(treatmentTypes, event.subSessionId)
                    .onSuccess { options ->
                        _uiState.update {
                            it.copy(
                                multimediaOptions = options,
                                currentMultimediaIndex = event.startIndex,
                                isLoading = false
                            )
                        }
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
                finish()
            }
        }
    }

    private fun goToNext() {
        val currentIndex = _uiState.value.currentMultimediaIndex
        val maxIndex = _uiState.value.multimediaOptions.lastIndex

        if (currentIndex < maxIndex) {
            _uiState.update { it.copy(currentMultimediaIndex = currentIndex + 1) }
        } else {
            finish()
        }
    }

    private fun goToPrevious() {
        val currentIndex = _uiState.value.currentMultimediaIndex
        if (currentIndex > 0) {
            _uiState.update { it.copy(currentMultimediaIndex = currentIndex - 1) }
        } else {
            // Optionally handle navigation back from the first item, for now we just stay
        }
    }

    private fun finish() {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateBack)
        }
    }

    sealed class NavigationEvent {
        object NavigateBack : NavigationEvent()
    }
}
