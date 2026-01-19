package com.colma.colmaApp.features.presion.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.presion.domain.PresionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class PresionViewModel(private val repository: PresionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PresionUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffects = Channel<SideEffect>()
    val sideEffects = _sideEffects.receiveAsFlow()

    init {
        loadHistory()
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
            is PresionEvent.SistolicaChanged -> {
                 if (event.value.length <= 3) {
                    _uiState.update { it.copy(sistolica = event.value, formError = null) }
                 }
            }
            is PresionEvent.DiastolicaChanged -> {
                if (event.value.length <= 3) {
                    _uiState.update { it.copy(diastolica = event.value, formError = null) }
                }
            }
            is PresionEvent.DateChanged -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = _uiState.value.measurementTime }
                calendar.set(Calendar.YEAR, event.year)
                calendar.set(Calendar.MONTH, event.month)
                calendar.set(Calendar.DAY_OF_MONTH, event.day)
                _uiState.update { it.copy(measurementTime = calendar.timeInMillis) }
            }
            is PresionEvent.TimeChanged -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = _uiState.value.measurementTime }
                calendar.set(Calendar.HOUR_OF_DAY, event.hour)
                calendar.set(Calendar.MINUTE, event.minute)
                _uiState.update { it.copy(measurementTime = calendar.timeInMillis) }
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
        }
    }

    private fun savePresion() {
        val state = _uiState.value
        val sys = state.sistolica.trim().replace(',', '.').toDoubleOrNull()
        val dia = state.diastolica.trim().replace(',', '.').toDoubleOrNull()

        if (sys == null || dia == null) {
            _uiState.update { it.copy(formError = "Ingrese números válidos") }
            return
        }
        if (sys <= dia) {
            _uiState.update { it.copy(formError = "El valor mayor debe ser mayor que el menor") }
            return
        }
        if (sys < 40.0 || sys > 300.0) {
            _uiState.update { it.copy(formError = "Valor imposible: presión sistólica fuera de rango plausible.") }
            return
        }
        if (dia < 20.0 || dia > 200.0) {
            _uiState.update { it.copy(formError = "Valor imposible: presión diastólica fuera de rango plausible.") }
            return
        }

        _uiState.update { it.copy(formError = null) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.savePresion(
                sistolica = sys,
                diastolica = dia,
                dateTime = state.measurementTime
            )

            result.onSuccess { newEntry ->
                val updatedHistory = listOf(newEntry) + state.history
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFormVisible = false,
                        history = updatedHistory,
                        lastPresion = newEntry,
                        error = null
                    )
                }
                _sideEffects.send(SideEffect.ShowToast("Guardado: Presión = ${newEntry.value}"))

                val highS = sys > 140
                val lowS = sys < 90
                val highD = dia > 90
                val lowD = dia < 60
                val parts = mutableListOf<String>()
                if (lowS || lowD) parts.add("presión baja")
                if (highS || highD) parts.add("presión alta")
                if (parts.isNotEmpty()) {
                    val warningText = "Advertencia: " + parts.joinToString(" y ") + ". Por favor consulte con un médico."
                    _sideEffects.send(SideEffect.ShowToast(warningText, Toast.LENGTH_LONG))
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                _sideEffects.send(SideEffect.ShowToast("Error: ${error.message}"))
            }
        }
    }

    sealed class SideEffect {
        data class ShowToast(val message: String, val duration: Int = Toast.LENGTH_SHORT) : SideEffect()
    }
}
