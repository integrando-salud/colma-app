package com.colma.colmaApp.features.presion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.presion.domain.PresionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class PresionViewModel(private val repository: PresionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PresionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadHistory()
        loadUserPaisId()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val history = repository.getHistory()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    history = history,
                    lastPresion = history.firstOrNull()
                )
            }
        }
    }

    private fun loadUserPaisId() {
        viewModelScope.launch {
            val paisId = repository.getUserPaisId()
            _uiState.update { it.copy(userPaisId = paisId) }
        }
    }

    fun onEvent(event: PresionEvent) {
        when (event) {
            PresionEvent.AddPresionClicked -> {
                _uiState.update {
                    it.copy(
                        isFormVisible = true,
                        sistolica = "",
                        diastolica = "",
                        measurementTime = System.currentTimeMillis(),
                        formError = null
                    )
                }
            }
            PresionEvent.DismissFormDialog -> {
                _uiState.update { it.copy(isFormVisible = false) }
            }
            PresionEvent.DismissSuccessDialog -> {
                _uiState.update { it.copy(showSuccessDialog = false) }
            }
            is PresionEvent.SistolicaChanged -> {
                val value = event.value.filter { it.isDigit() }
                 if (value.length <= 3) {
                    _uiState.update { it.copy(sistolica = value, formError = null) }
                 }
            }
            is PresionEvent.DiastolicaChanged -> {
                val value = event.value.filter { it.isDigit() }
                if (value.length <= 3) {
                    _uiState.update { it.copy(diastolica = value, formError = null) }
                }
            }
            is PresionEvent.DateChanged -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = _uiState.value.measurementTime }
                calendar.set(Calendar.YEAR, event.year)
                calendar.set(Calendar.MONTH, event.month)
                calendar.set(Calendar.DAY_OF_MONTH, event.day)
                val newTime = if (calendar.timeInMillis > System.currentTimeMillis()) System.currentTimeMillis() else calendar.timeInMillis
                _uiState.update { it.copy(measurementTime = newTime) }
            }
            is PresionEvent.TimeChanged -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = _uiState.value.measurementTime }
                calendar.set(Calendar.HOUR_OF_DAY, event.hour)
                calendar.set(Calendar.MINUTE, event.minute)
                val newTime = if (calendar.timeInMillis > System.currentTimeMillis()) System.currentTimeMillis() else calendar.timeInMillis
                _uiState.update { it.copy(measurementTime = newTime) }
            }
            PresionEvent.SavePresionClicked -> {
                savePresion()
            }
            is PresionEvent.ShowDetails -> {
                _uiState.update { it.copy(isDetailsDialogVisible = true, selectedEntry = event.entry) }
            }
            PresionEvent.DismissDetailsDialog -> {
                _uiState.update { it.copy(isDetailsDialogVisible = false, selectedEntry = null) }
            }
            PresionEvent.DismissWarningDialog -> {
                _uiState.update { it.copy(warningDialog = PresionUiState.WarningDialog(show = false)) }
            }
        }
    }

    private fun savePresion() {
        val state = _uiState.value
        val sys = state.sistolica.trim().toIntOrNull()
        val dia = state.diastolica.trim().toIntOrNull()

        if (sys == null || dia == null) {
            _uiState.update { it.copy(formError = "Ingrese números válidos") }
            return
        }
        if (state.sistolica.length < 2 || state.diastolica.length < 2) {
            _uiState.update { it.copy(formError = "El valor debe tener al menos 2 dígitos") }
            return
        }
        if (sys <= dia) {
            _uiState.update { it.copy(formError = "El valor mayor debe ser mayor que el menor") }
            return
        }
        if (sys < 40 || sys > 300) {
            _uiState.update { it.copy(formError = "Valor imposible: presión sistólica fuera de rango plausible.") }
            return
        }
        if (dia < 20 || dia > 200) {
            _uiState.update { it.copy(formError = "Valor imposible: presión diastólica fuera de rango plausible.") }
            return
        }

        _uiState.update { it.copy(formError = null) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val highS = sys > 140
            val lowS = sys < 90
            val highD = dia > 90
            val lowD = dia < 60

            val advertencia = when {
                highS || highD -> "Alta"
                lowS || lowD -> "Baja"
                else -> "Normal"
            }

            val result = repository.savePresion(
                sistolica = sys.toDouble(),
                diastolica = dia.toDouble(),
                dateTime = state.measurementTime,
                advertencia = advertencia
            )

            result.onSuccess { newEntry ->
                val updatedHistory = (listOf(newEntry) + state.history).sortedByDescending { it.timestamp }

                val severeHigh = sys >= 180 || dia >= 120
                val severeLow = sys <= 90
                val highS = sys > 140
                val lowS = sys < 90
                val highD = dia > 90
                val lowD = dia < 60

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFormVisible = false,
                        history = updatedHistory,
                        lastPresion = updatedHistory.firstOrNull(),
                        error = null,
                        showSuccessDialog = !severeHigh && !severeLow && !highS && !lowS && !highD && !lowD
                    )
                }

                // Attempt to sync
                viewModelScope.launch {
                    repository.syncPresion(newEntry, advertencia)
                }

                when {
                    severeHigh -> {
                        val warningMessage = getSevereHighPressureWarning(_uiState.value.userPaisId)
                        _uiState.update { it.copy(warningDialog = PresionUiState.WarningDialog(show = true, message = warningMessage)) }
                    }
                    severeLow -> {
                        val warningMessage = getSevereLowPressureWarning(_uiState.value.userPaisId)
                        _uiState.update { it.copy(warningDialog = PresionUiState.WarningDialog(show = true, message = warningMessage)) }
                    }
                    highS || highD || lowS || lowD -> {
                        val parts = mutableListOf<String>()
                        if (lowS || lowD) parts.add("presión baja")
                        if (highS || highD) parts.add("presión alta")
                        val warningText = "Advertencia: " + parts.joinToString(" y ") + ". Por favor consulte con un médico."
                        _uiState.update { it.copy(warningDialog = PresionUiState.WarningDialog(show = true, message = warningText)) }
                    }
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

    private fun getSevereHighPressureWarning(paisId: Int?): String {
        return when (paisId) {
            1 -> {
                "Los valores de presión que ingresaste son altos, es necesario que consultes a una guardia/ Urgencias para recibir atención médica.\n" +
                        "También podés llamar al 107."
            }
            2 -> {
                "La medición de presión que registraste es alta, es importante que acudas a emergencia para recibir atención médica."
            }
            else -> {
                "Advertencia: Presión arterial severamente alta. Por favor consulte con un médico urgentemente."
            }
        }
    }

    private fun getSevereLowPressureWarning(paisId: Int?): String {
        return when (paisId) {
            1 -> {
                "Los valores de presión que ingresaste son bajos, si tenés algún síntoma de malestar o desmayo es importante que consultes a una guardia para recibir atención médica.\n" +
                        "También podés llamar al 107."
            }
            2 -> {
                "La medición de presión que registraste es baja, si sientes malestar físico o has tenido un desmayo, es importante que vayas a tu centro de salud o a emergencia para recibir atención médica."
            }
            else -> {
                "Advertencia: Presión arterial severamente baja. Por favor consulte con un médico urgentemente."
            }
        }
    }
}
