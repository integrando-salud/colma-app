package com.codeco.tabacaleros.features.DNI.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeco.tabacaleros.features.DNI.domain.DNIRepository
import com.codeco.tabacaleros.servicios.SessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DNIViewModel(
    private val repository: DNIRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DniUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionManager.selectedRadicacionFlow.collect { radicacion ->
                _uiState.update { it.copy(selectedCasaColonoName = radicacion?.descripcion) }
            }
        }
    }

    fun onEvent(event: DniEvent) {
        when (event) {
            is DniEvent.DocumentNumberChanged -> {
                _uiState.update { it.copy(documentNumber = event.number) }
                val digitsOnly = event.number.filter { it.isDigit() }
                if (digitsOnly.length in 11..12) {
                    getProductorInfo(digitsOnly)
                } else {
                     _uiState.update { it.copy(productorInfo = null, error = null) }
                }
            }
            is DniEvent.PhoneNumberChanged -> {
                _uiState.update { it.copy(phoneNumber = event.phone) }
            }
            is DniEvent.PhoneVerificationChanged -> {
                _uiState.update { it.copy(phoneVerificationResult = event.isVerified) }
            }
            DniEvent.ContinueClicked -> {
                onContinueClicked()
            }
            DniEvent.ScreenTouched -> {
                _uiState.update { it.copy(showTouched = true) }
            }
            DniEvent.WarningDialogDismissed -> {
                _uiState.update { it.copy(showWarningDialog = false) }
            }
        }
    }

    private fun getProductorInfo(cuil: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getProductorInfo(cuil)
                .onSuccess { productorInfo ->
                    _uiState.update {
                        it.copy(
                            productorInfo = productorInfo,
                            isLoading = false,
                            phoneVerificationResult = null
                        )
                    }
                    getActividadesInfo(productorInfo.idProductor)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isLoading = false,
                            productorInfo = null
                        )
                    }
                }
        }
    }

    private fun getActividadesInfo(idProductor: Int) {
        viewModelScope.launch {
            repository.getActividades(idProductor)
                .onSuccess { response ->
                    val totalActividades = response.actividades.size
                    val sinRegistrar = response.actividades.count { it.snCensado == 0 }

                    if (totalActividades <= 0) {
                        _uiState.update {
                            it.copy(
                                showWarningDialog = true,
                                dialogMessage = "El usuario no tiene actividades"
                            )
                        }
                    } else if (sinRegistrar == 0) {
                        _uiState.update {
                            it.copy(
                                showWarningDialog = true,
                                dialogMessage = "Todas las actividades se encuentran registradas"
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = "Error al obtener actividades: ${error.message}") }
                }
        }
    }

    private fun onContinueClicked() {
        val state = _uiState.value
        val digitsOnly = state.documentNumber.filter{it.isDigit()}
        val isValid = digitsOnly.length in 11..12

        if (isValid) {
            viewModelScope.launch {
                state.productorInfo?.let {
                    val infoToSave = it.copy(
                        productorPhone = state.phoneNumber,
                        phoneVerification = state.phoneVerificationResult
                    )
                    repository.saveProductorInfo(infoToSave)
                }
                _navigationEvent.send(NavigationEvent.NavigateToNextScreen(digitsOnly))
            }
        }
    }

    sealed class NavigationEvent {
        data class NavigateToNextScreen(val cuil: String) : NavigationEvent()
    }
}
