package com.colma.colmaApp.features.MenuMain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.MenuMain.domain.MenuMainRepository
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MenuMainViewModel(private val repository: MenuMainRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuMainUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        onEvent(MenuMainEvent.LoadInitialData)
    }

    fun onEvent(event: MenuMainEvent) {
        when (event) {
            MenuMainEvent.LoadInitialData -> loadInitialData()
            MenuMainEvent.OnSessionsClick -> navigateToSessions()
            MenuMainEvent.OnHealthRegistersClick -> navigateToHealthRegisters()
            MenuMainEvent.OnMetasHistoryClick -> navigateToMetasHistory()
            MenuMainEvent.OnSalirClick -> navigateToLogin()
            MenuMainEvent.OnSyncClick -> synchronizeData()
            MenuMainEvent.ObjectiveDialogDismissed -> dismissObjectiveDialog()
            MenuMainEvent.ObjectiveCompleted -> handleObjectiveCompleted()
            is MenuMainEvent.ObjectiveNotCompleted -> handleObjectiveNotCompleted(event.reason)
            is MenuMainEvent.OtherReasonChanged -> updateOtherReason(event.reason)
            MenuMainEvent.OtherReasonConfirmed -> confirmOtherReason()
            MenuMainEvent.SnackbarDismissed -> dismissSnackbar()
        }
    }

        private fun loadInitialData() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val pendingPastMeta = repository.getPendingPastMeta()
                val pendingMetasCount = repository.getPendingFutureMetasCount()
    
                _uiState.update {
                    it.copy(
                        pendingMetasCount = pendingMetasCount,
                        todaysObjective = pendingPastMeta,
                        showObjectiveDialog = pendingPastMeta != null,
                        isLoading = false
                    )
                }
            }
        }
    
        private fun navigateToSessions() {
            viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateToSessions) }
        }
    
        private fun navigateToHealthRegisters() {
            viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateToHealthRegisters) }
        }
    
        private fun navigateToMetasHistory() {
            viewModelScope.launch { _navigationEvent.send(NavigationEvent.NavigateToMetasHistory) }
        }
    
            private fun navigateToLogin() {
                viewModelScope.launch {
                    repository.logout()
                    _navigationEvent.send(NavigationEvent.NavigateToLogin)
                }
            }    
        private fun synchronizeData() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, snackbarMessage = "Sincronizando datos...") }
                val result = repository.synchronizeData()
                result.onSuccess {
                    _uiState.update { it.copy(snackbarMessage = "Sincronización exitosa!") }
                }.onFailure { error ->
                    _uiState.update { it.copy(snackbarMessage = "Error de sincronización: ${error.message}") }
                }
                _uiState.update { it.copy(isLoading = false) }
                loadInitialData() // Reload data after sync to update counts
            }
        }
    private fun dismissObjectiveDialog() {
        _uiState.update { it.copy(showObjectiveDialog = false, showReasonDialog = false, showOtherReasonDialog = false) }
    }

    private fun handleObjectiveCompleted() {
        viewModelScope.launch {
            _uiState.value.todaysObjective?.let { currentMeta ->
                val updatedMeta = currentMeta.copy(meta_cumplida = 2) // Assuming '2' means completed
                repository.updateMeta(updatedMeta)
            }
            _uiState.update { it.copy(showObjectiveDialog = false, todaysObjective = null) }
            loadInitialData() // Update counts
        }
    }

    private fun handleObjectiveNotCompleted(reason: String) {
        if (reason.isEmpty()) {
            _uiState.update { it.copy(showObjectiveDialog = false, showReasonDialog = true) }
        } else if (reason == "otro") {
            _uiState.update { it.copy(showReasonDialog = false, showOtherReasonDialog = true, selectedReason = "otro") }
        } else {
            _uiState.update { it.copy(selectedReason = reason) }
            confirmOtherReason()
        }
    }

    private fun updateOtherReason(reason: String) {
        _uiState.update { it.copy(otherReasonInput = reason) }
    }

    private fun confirmOtherReason() {
        viewModelScope.launch {
            val reason = if (_uiState.value.selectedReason == "otro") {
                _uiState.value.otherReasonInput
            } else {
                _uiState.value.selectedReason
            }

            _uiState.value.todaysObjective?.let { currentMeta ->
                val updatedMeta = currentMeta.copy(
                    meta_cumplida = 1, // Assuming '1' means not completed
                    motivo_incumplimiento = reason
                )
                repository.updateMeta(updatedMeta)
            }
            _uiState.update {
                it.copy(
                    showReasonDialog = false,
                    showOtherReasonDialog = false,
                    todaysObjective = null,
                    selectedReason = "",
                    otherReasonInput = ""
                )
            }
            loadInitialData() // Update counts
        }
    }

    private fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    sealed class NavigationEvent {
        object NavigateToSessions : NavigationEvent()
        object NavigateToHealthRegisters : NavigationEvent()
        object NavigateToMetasHistory : NavigationEvent()
        object NavigateToLogin : NavigationEvent()
    }
}
