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
            GlucosaEvent.DismissSuccessDialog -> {
                _uiState.update { it.copy(showSuccessDialog = false) }
            }
            is GlucosaEvent.GlucosaValueChanged -> {
                val value = event.value.filter { it.isDigit() }
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
                val newTime = if (calendar.timeInMillis > System.currentTimeMillis()) System.currentTimeMillis() else calendar.timeInMillis
                _uiState.update { it.copy(measurementTime = newTime) }
            }
            is GlucosaEvent.TimeChanged -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = _uiState.value.measurementTime }
                calendar.set(Calendar.HOUR_OF_DAY, event.hour)
                calendar.set(Calendar.MINUTE, event.minute)
                val newTime = if (calendar.timeInMillis > System.currentTimeMillis()) System.currentTimeMillis() else calendar.timeInMillis
                _uiState.update { it.copy(measurementTime = newTime) }
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
            GlucosaEvent.DismissWarningDialog -> {
                _uiState.update { it.copy(warningDialog = GlucosaUiState.WarningDialog(show = false)) }
            }
        }
    }

    private fun saveGlucosa() {
        val state = _uiState.value
        val glucosaStr = state.glucosaValue.trim()
        val number = glucosaStr.toIntOrNull()

        if (number == null) {
            _uiState.update { it.copy(formError = "Ingrese un número válido") }
            return
        }
        if (glucosaStr.length < 2) {
            _uiState.update { it.copy(formError = "Ingrese al menos 2 caracteres") }
            return
        }
        if (state.isFasting == null) {
            _uiState.update { it.copy(formError = "Seleccione si está en ayuno o no") }
            return
        }
        if (number < 10) {
            _uiState.update { it.copy(formError = "Valor imposible: Glucosa demasiado baja para ser plausible.") }
            return
        }
        if (number > 1000) {
            _uiState.update { it.copy(formError = "Valor imposible: Glucosa extremadamente alta para ser plausible.") }
            return
        }

        _uiState.update { it.copy(formError = null) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.saveGlucosa(
                value = number.toDouble(),
                isFasting = state.isFasting,
                dateTime = state.measurementTime
            )

            result.onSuccess { newEntry ->
                val updatedHistory = listOf(newEntry) + state.history
                
                val low = number <= 69
                val high = if (state.isFasting == true) number >= 131 else number >= 181
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFormVisible = false,
                        history = updatedHistory,
                        lastGlucosa = newEntry,
                        error = null,
                        showSuccessDialog = !low && !high
                    )
                }

                when {
                    low -> _uiState.update { it.copy(warningDialog = GlucosaUiState.WarningDialog(show = true, message = "Advertencia: Glucosa baja (hipoglucemia). Por favor consulte con un médico.")) }
                    high -> _uiState.update { it.copy(warningDialog = GlucosaUiState.WarningDialog(show = true, message = "Advertencia: Glucosa alta (hiperglucemia). Por favor consulte con un médico.")) }
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
}
