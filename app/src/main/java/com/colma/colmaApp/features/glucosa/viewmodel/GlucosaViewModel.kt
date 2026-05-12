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
                    lastGlucosa = history.firstOrNull()
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

            val low = number <= 69
            val high = if (state.isFasting == true) number >= 126 else number >= 180
            val severeHigh = number > 300
            val advertencia = when {
                low -> "Baja"
                high -> "Alta"
                else -> "Normal"
            }

            val result = repository.saveGlucosa(
                value = number.toDouble(),
                isFasting = state.isFasting,
                dateTime = state.measurementTime,
                advertencia = advertencia
            )

            result.onSuccess { newEntry ->
                val updatedHistory = (listOf(newEntry) + state.history).sortedByDescending { it.timestamp }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFormVisible = false,
                        history = updatedHistory,
                        lastGlucosa = updatedHistory.firstOrNull(),
                        error = null,
                        showSuccessDialog = !low && !high
                    )
                }

                // Attempt to sync
                viewModelScope.launch {
                    repository.syncGlucosa(newEntry, advertencia)
                }

                when {
                    low -> {
                        val warningMessage = getLowGlucosaWarning(state.isFasting == true, _uiState.value.userPaisId)
                        _uiState.update { it.copy(warningDialog = GlucosaUiState.WarningDialog(show = true, message = warningMessage)) }
                    }
                    severeHigh -> {
                        val warningMessage = getHighGlucosaWarning(state.isFasting == true, _uiState.value.userPaisId)
                        _uiState.update { it.copy(warningDialog = GlucosaUiState.WarningDialog(show = true, message = warningMessage)) }
                    }
                    high -> {
                        _uiState.update { it.copy(warningDialog = GlucosaUiState.WarningDialog(show = true, message = "Advertencia: Glucosa alta (hiperglucemia). Por favor consulte con un médico.")) }
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

    private fun getLowGlucosaWarning(isFasting: Boolean, paisId: Int?): String {
        return when (paisId) {
            1 -> {
                "Tu nivel de azúcar está bajo.\n" +
                        "Si tenés temblores, sudor frío, mareo o debilidad, segui el consejo del equipo medico: comé un caramelo o tomá un vaso de jugo o gaseosa común.\n" +
                        "Si te seguís sintiendo mal o el valor sigue bajo, consultá con tu médico o andá a una guardia. También podés llamar al 107."
            }
            2 -> {
                if (isFasting) {
                    "La medición de azúcar que registraste es baja, vuelve medirte en otro dedo. Si la medición sigue siendo baja, sigue las indicaciones de tu médico para estos casos y saca una cita en tu centro de salud para que te evalúen."
                } else {
                    "La medición de azúcar que registraste es baja, vuelve medirte en otro dedo. Si la medición sigue siendo baja, saca una cita con tu médico para que te evalúe."
                }
            }
            else -> {
                "Advertencia: Glucosa baja (hipoglucemia). Por favor consulte con un médico."
            }
        }
    }

    private fun getHighGlucosaWarning(isFasting: Boolean, paisId: Int?): String {
        return when (paisId) {
            1 -> {
                "Tu azúcar está muy alta. Si sentís náuseas, vómitos, dolor abdominal, orinas muy seguido o te sientes muy mal es necesario que consultes a una guardia/ Urgencias.\n" +
                        "También podés llamar al 107."
            }
            2 -> {
                "La medición de azúcar que registraste es alta. Si sientes náuseas, vómitos, dolor abdominal, orinas muy seguido o te sientes muy mal es necesario que acudas a tu centro de salud o a emergencia para que recibas atención médica."
            }
            else -> {
                "Advertencia: Glucosa alta (hiperglucemia). Por favor consulte con un médico."
            }
        }
    }
}
