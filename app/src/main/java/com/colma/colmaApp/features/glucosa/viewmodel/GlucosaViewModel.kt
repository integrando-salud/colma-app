package com.colma.colmaApp.features.glucosa.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.glucosa.domain.GlucosaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class GlucosaViewModel(private val repository: GlucosaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GlucosaUiState())
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
                    lastGlucosa = history.firstOrNull()
                )
            }
        }
    }

    fun onEvent(event: GlucosaEvent) {
        when (event) {
            GlucosaEvent.AddGlucosaClicked -> {
                _uiState.update {
                    it.copy(
                        isFormVisible = true,
                        glucosaValue = "",
                        isFasting = null,
                        measurementTime = System.currentTimeMillis(),
                        formError = null
                    )
                }
            }
            GlucosaEvent.DismissFormDialog -> {
                _uiState.update { it.copy(isFormVisible = false) }
            }
            is GlucosaEvent.GlucosaValueChanged -> {
                val value = event.value
                if (value.length <= 3) {
                    _uiState.update { it.copy(glucosaValue = value, formError = null) }
                }
            }
            is GlucosaEvent.FastingChanged -> {
                _uiState.update { it.copy(isFasting = event.isFasting, formError = null) }
            }
            is GlucosaEvent.DateChanged -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = _uiState.value.measurementTime }
                calendar.set(Calendar.YEAR, event.year)
                calendar.set(Calendar.MONTH, event.month)
                calendar.set(Calendar.DAY_OF_MONTH, event.day)
                _uiState.update { it.copy(measurementTime = calendar.timeInMillis) }
            }
            is GlucosaEvent.TimeChanged -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = _uiState.value.measurementTime }
                calendar.set(Calendar.HOUR_OF_DAY, event.hour)
                calendar.set(Calendar.MINUTE, event.minute)
                _uiState.update { it.copy(measurementTime = calendar.timeInMillis) }
            }
            GlucosaEvent.SaveGlucosaClicked -> {
                saveGlucosa()
            }
            is GlucosaEvent.ShowDetails -> {
                _uiState.update { it.copy(isDetailsDialogVisible = true, selectedEntry = event.entry) }
            }
            GlucosaEvent.DismissDetailsDialog -> {
                _uiState.update { it.copy(isDetailsDialogVisible = false, selectedEntry = null) }
            }
        }
    }

    private fun saveGlucosa() {
        val state = _uiState.value
        val number = state.glucosaValue.trim().replace(',', '.').toDoubleOrNull()

        if (number == null) {
            _uiState.update { it.copy(formError = "Ingrese un número válido") }
            return
        }
        if (state.isFasting == null) {
            _uiState.update { it.copy(formError = "Seleccione si está en ayuno o no") }
            return
        }
        if (number < 10.0) {
            _uiState.update { it.copy(formError = "Valor imposible: Glucosa demasiado baja para ser plausible.") }
            return
        }
        if (number > 1000.0) {
            _uiState.update { it.copy(formError = "Valor imposible: Glucosa extremadamente alta para ser plausible.") }
            return
        }

        _uiState.update { it.copy(formError = null) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.saveGlucosa(
                value = number,
                isFasting = state.isFasting,
                dateTime = state.measurementTime
            )

            result.onSuccess { newEntry ->
                val updatedHistory = listOf(newEntry) + state.history
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFormVisible = false,
                        history = updatedHistory,
                        lastGlucosa = newEntry,
                        error = null
                    )
                }
                _sideEffects.send(SideEffect.ShowToast("Guardado: Glucosa = ${newEntry.value}"))

                val low = number <= 69.0
                val high = if (state.isFasting) number >= 131.0 else number >= 181.0
                when {
                    low -> _sideEffects.send(SideEffect.ShowToast("Advertencia: Glucosa baja (hipoglucemia). Por favor consulte con un médico.", Toast.LENGTH_LONG))
                    high -> _sideEffects.send(SideEffect.ShowToast("Advertencia: Glucosa alta (hiperglucemia). Por favor consulte con un médico.", Toast.LENGTH_LONG))
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
