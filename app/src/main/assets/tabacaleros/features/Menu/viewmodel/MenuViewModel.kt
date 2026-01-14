package com.codeco.tabacaleros.features.Menu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeco.tabacaleros.features.Menu.domain.MenuRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MenuViewModel(private val menuRepository: MenuRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val components = menuRepository.getComponents()
            val radicacion = menuRepository.getSelectedRadicacion()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    components = components ?: emptyList(),
                    radicacionName = radicacion?.descripcion
                )
            }
        }
    }

    fun onEvent(event: MenuEvent) {
        when (event) {
            MenuEvent.OnRegistroClicked -> {
                onRegistroClicked()
            }
            MenuEvent.OnConsultaClicked -> {
                onConsultaClicked()
            }
        }
    }

    private fun onRegistroClicked() {
        viewModelScope.launch {
            menuRepository.clearProducerInfo()
            _navigationEvent.send(NavigationEvent.NavigateToDni)
        }
    }

    private fun onConsultaClicked() {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateToDniConsulta)
        }
    }

    sealed class NavigationEvent {
        object NavigateToDni : NavigationEvent()
        object NavigateToDniConsulta : NavigationEvent()
    }
}
