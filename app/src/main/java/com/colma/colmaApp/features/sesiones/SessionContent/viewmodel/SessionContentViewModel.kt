package com.colma.colmaApp.features.sesiones.SessionContent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.sesiones.SessionContent.domain.SessionContentRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionContentViewModel(private val repository: SessionContentRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionContentUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun onEvent(event: SessionContentEvent) {
        when (event) {
            is SessionContentEvent.LoadSession -> {
                _uiState.update {
                    it.copy(
                        sessionId = event.sessionId,
                        sessionTitle = event.sessionTitle,
                        subSessionId = event.subSessionId,
                        subSessionTitle = event.subSessionTitle,
                        isLoading = true
                    )
                }
                loadContent()
            }
            is SessionContentEvent.TabSelected -> {
                _uiState.update { it.copy(selectedTab = event.index) }
            }
            SessionContentEvent.BackClicked -> {
                viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateBack) }
            }
            SessionContentEvent.TerminarClicked -> {
                viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateBack) }
            }
        }
    }

    private fun loadContent() {
        viewModelScope.launch {
            val result = repository.getSessionContent(_uiState.value.sessionId, _uiState.value.subSessionId)
            result.onSuccess { content ->
                _uiState.update {
                    it.copy(
                        informationContent = content.information,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        error = error.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    sealed class NavigationEvent {
        object NavigateBack : NavigationEvent()
    }
}
