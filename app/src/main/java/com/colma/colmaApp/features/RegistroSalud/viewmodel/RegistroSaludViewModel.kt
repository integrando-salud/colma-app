package com.colma.colmaApp.features.RegistroSalud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.RegistroSalud.domain.RegistroSaludRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RegistroSaludViewModel(private val repository: RegistroSaludRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroSaludUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun onEvent(event: RegistroSaludEvent) {
        when (event) {
            RegistroSaludEvent.OnGlucosaClicked -> navigateTo(NavigationEvent.NavigateToGlucosa)
            RegistroSaludEvent.OnPresionClicked -> navigateTo(NavigationEvent.NavigateToPresion)
            RegistroSaludEvent.OnEstadoAnimoClicked -> navigateTo(NavigationEvent.NavigateToEstadoAnimo)
            RegistroSaludEvent.OnBackClicked -> navigateTo(NavigationEvent.NavigateBack)
        }
    }

    private fun navigateTo(event: NavigationEvent) {
        viewModelScope.launch {
            _navigationEvent.send(event)
        }
    }

    sealed class NavigationEvent {
        object NavigateToGlucosa : NavigationEvent()
        object NavigateToPresion : NavigationEvent()
        object NavigateToEstadoAnimo : NavigationEvent()
        object NavigateBack : NavigationEvent()
    }
}
