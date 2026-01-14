package com.codeco.tabacaleros.features.casaColono.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeco.tabacaleros.features.casaColono.domain.CasaColonoRepository
import com.codeco.tabacaleros.servicios.SessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CasaColonoViewModel(
    private val repository: CasaColonoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CasaColonoUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        getRadicaciones()
    }

    private fun getRadicaciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.getRadicaciones()
            result.onSuccess { radicaciones ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        radicaciones = radicaciones
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            }
        }
    }
    
    fun onEvent(event: CasaColonoEvent) {
        when (event) {
            is CasaColonoEvent.OnRadicacionClicked -> {
                viewModelScope.launch {
                    sessionManager.saveSelectedRadicacion(event.radicacion)
                    _navigationEvent.send(NavigationEvent.NavigateToMenu)
                }
            }
        }
    }
    
    sealed class NavigationEvent {
        object NavigateToMenu : NavigationEvent()
    }
}
