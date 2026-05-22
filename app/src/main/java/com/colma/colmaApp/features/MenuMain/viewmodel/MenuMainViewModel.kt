package com.colma.colmaApp.features.MenuMain.viewmodel

import android.util.Log
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
import java.io.IOException

class MenuMainViewModel(private val repository: MenuMainRepository) : ViewModel() {

    private val TAG = "MenuMainViewModel"

    private val _uiState = MutableStateFlow(MenuMainUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        onEvent(MenuMainEvent.LoadInitialData)
        autoSyncOnStartup()
    }

    private val reasonMessages = mapOf(
        "No tenía energía / ánimo para hacerlo" to "Cuando te sientas sin energía o con mucho cansancio, es importante que vayas a tu propio ritmo y planifiques tus actividades de tal manera que no tengas que hacer todo al mismo tiempo, sino poco a poco",
        "Otras actividades me quitaron tiempo (trabajo, familia, etc.)" to "¡No te rindas! Si afrontas una situación así en el futuro, piensa en cómo puedes hacerlo más fácil para ti, planeando tu tiempo, no haciendo demasiadas cosas a la vez, etc.",
        "Tuve dificultades para hacer la actividad" to "¡No te rindas! ¡Sigue intentando! Intenta dividir la actividad en pequeñitos pasos o intenta una actividad más fácil esta semana.",
        "Tuve un problema de salud / estuve enfermo(a)" to "Esperamos que te sientas mejor. Intenta escoger otra actividad para esta semana. Si tu malestar se mantiene, acude a tu centro de salud",
        "La persona con quien iba a hacer la actividad no pudo acompañarme" to "Coordinar con alguien puede ser complicado, pero no te rindas. Vale la pena intentar un par de veces más, ¡y seguro encuentras el momento perfecto para hacerlo juntos!",
        "Me olvidé de hacer la actividad." to "¡A todos nos pasa! Para la próxima, puedes probar anotarlo o pedirle a alguien que te lo recuerde. ¡Verás que así será más fácil cumplir tu meta!"
    )

    fun onEvent(event: MenuMainEvent) {
        when (event) {
            MenuMainEvent.LoadInitialData -> loadInitialData()
            MenuMainEvent.OnSessionsClick -> navigateToSessions()
            MenuMainEvent.OnHealthRegistersClick -> navigateToHealthRegisters()
            MenuMainEvent.OnMetasHistoryClick -> navigateToMetasHistory()
            MenuMainEvent.OnSalirClick -> navigateToLogin()
            MenuMainEvent.OnSyncClick -> synchronizeData()
            MenuMainEvent.OnRequestHelpClick -> requestHelp()
            MenuMainEvent.ObjectiveDialogDismissed -> dismissObjectiveDialog()
            MenuMainEvent.ObjectiveCompleted -> handleObjectiveCompleted()
            MenuMainEvent.DismissFeedbackDialog -> dismissFeedbackDialog()
            is MenuMainEvent.ObjectiveNotCompleted -> handleObjectiveNotCompleted(event.reason)
            is MenuMainEvent.OtherReasonChanged -> updateOtherReason(event.reason)
            MenuMainEvent.OtherReasonConfirmed -> confirmOtherReason()
            MenuMainEvent.SnackbarDismissed -> dismissSnackbar()
            MenuMainEvent.DismissNoInternetDialog -> dismissNoInternetDialog()
        }
    }

        private fun loadInitialData() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val pendingPastMeta = repository.getPendingPastMeta()
                val pendingMetasCount = repository.getPendingFutureMetasCount()
                val userName = repository.getUserName() ?: "Usuario"

                _uiState.update {
                    it.copy(
                        pendingMetasCount = pendingMetasCount,
                        todaysObjective = pendingPastMeta,
                        showObjectiveDialog = pendingPastMeta != null,
                        userName = userName,
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
                _uiState.update { it.copy(isLoading = true) }
                repository.synchronizeData()
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, syncSuccessful = true) }
                        kotlinx.coroutines.delay(3000)
                        _uiState.update { it.copy(syncSuccessful = false) }
                        loadInitialData() // Reload data after sync to update counts
                    }
                    .onFailure { error ->
                        if (error is IOException && error.message == "No internet connection") {
                            _uiState.update { it.copy(isLoading = false, showNoInternetDialog = true) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, snackbarMessage = error.message ?: "Error de sincronización") }
                        }
                        Log.e(TAG, "Sync failed: ${error.message}")
                    }
            }
        }

        private fun requestHelp() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                repository.requestHelp()
                    .onSuccess { message ->
                        _uiState.update { it.copy(isLoading = false, snackbarMessage = message) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, snackbarMessage = error.message ?: "Error al solicitar ayuda") }
                        Log.e(TAG, "Request help failed: ${error.message}")
                    }
            }
        }

        private fun autoSyncOnStartup() {
            viewModelScope.launch {
                Log.d(TAG, "Auto-sync triggered on app startup")
                repository.synchronizeData()
                    .onSuccess { message ->
                        Log.d(TAG, "Auto-sync successful: $message")
                        // Reload data after successful sync
                        loadInitialData()
                    }
                    .onFailure { error ->
                        Log.w(TAG, "Auto-sync failed (non-critical): ${error.message}")
                    }
            }
        }
    private fun dismissObjectiveDialog() {
        _uiState.update { it.copy(showObjectiveDialog = false, showReasonDialog = false, showOtherReasonDialog = false) }
    }

    private fun dismissFeedbackDialog() {
        _uiState.update { it.copy(showFeedbackDialog = false) }
        loadInitialData()
    }

    private fun handleObjectiveCompleted() {
        viewModelScope.launch {
            _uiState.value.todaysObjective?.let { currentMeta ->
                val updatedMeta = currentMeta.copy(meta_cumplida = 2) // Assuming '2' means completed
                repository.updateMeta(updatedMeta)
            }
            _uiState.update {
                it.copy(
                    showObjectiveDialog = false,
                    showFeedbackDialog = true,
                    isSuccessFeedback = true,
                    feedbackMessage = "¡Excelente! ¡Elige otra actividad para esta semana y sigue para adelante!",
                    todaysObjective = null
                )
            }
        }
    }

    private fun handleObjectiveNotCompleted(reason: String) {
        if (reason.isEmpty()) {
            _uiState.update { it.copy(showObjectiveDialog = false, showReasonDialog = true) }
        } else if (reason == "otro") {
            _uiState.update { it.copy(showReasonDialog = false, showOtherReasonDialog = true, selectedReason = "otro") }
        } else {
            val message = reasonMessages[reason] ?: "¡No te rindas! ¡Sigue intentando!"
            _uiState.update { it.copy(selectedReason = reason, feedbackMessage = message, isSuccessFeedback = false) }
            confirmOtherReason()
        }
    }

    private fun updateOtherReason(reason: String) {
        _uiState.update { it.copy(otherReasonInput = reason) }
    }

    private fun confirmOtherReason() {
        viewModelScope.launch {
            val isOther = _uiState.value.selectedReason == "otro"
            val reason = if (isOther) {
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

            val feedbackMsg = if (isOther) {
                "¡No te rindas! Sigue intentando y verás que poco a poco será más fácil cumplir tu meta."
            } else {
                _uiState.value.feedbackMessage
            }

            _uiState.update {
                it.copy(
                    showReasonDialog = false,
                    showOtherReasonDialog = false,
                    showFeedbackDialog = true,
                    isSuccessFeedback = false,
                    feedbackMessage = feedbackMsg,
                    todaysObjective = null,
                    selectedReason = "",
                    otherReasonInput = ""
                )
            }
        }
    }

    private fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun dismissNoInternetDialog() {
        _uiState.update { it.copy(showNoInternetDialog = false) }
    }

    sealed class NavigationEvent {
        object NavigateToSessions : NavigationEvent()
        object NavigateToHealthRegisters : NavigationEvent()
        object NavigateToMetasHistory : NavigationEvent()
        object NavigateToLogin : NavigationEvent()
    }
}
