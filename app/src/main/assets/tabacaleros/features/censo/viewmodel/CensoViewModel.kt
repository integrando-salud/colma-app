package com.codeco.tabacaleros.features.censo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeco.tabacaleros.features.DNI.model.ProductorInfo
import com.codeco.tabacaleros.features.censo.domain.CensoRepository
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesDesarrolloRequest
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadesRequest
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class CensoViewModel(private val repository: CensoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CensoUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        onEvent(CensoEvent.LoadData)
    }

    fun onEvent(event: CensoEvent) {
        when (event) {
            CensoEvent.LoadData -> loadInitialData()
            is CensoEvent.ToggleItemSelection -> toggleItemSelection(event.itemId)
            is CensoEvent.UpdatePlantas -> updateActividad(event.actividadId) { it.copy(censoPlantasProductor = event.plantas) }
            is CensoEvent.UpdateCuadros -> updateActividad(event.actividadId) { it.copy(censoCuadrosCantidad = event.cuadros) }
            is CensoEvent.UpdateKilos -> updateActividad(event.actividadId) { it.copy(censoKilosEstimadoProductor = event.kilos) }
            is CensoEvent.MarkAsCensado -> {
                val actividad = _uiState.value.actividades.find { it.id == event.actividadId }
                if (actividad != null) {
                    if (actividad.censoKilosEstimadoProductor < 100 || actividad.censoCuadrosCantidad < 1) {
                        _uiState.update { it.copy(error = "Para registrar, los kilos estimados deben ser 100 o más y los cuadros 1 o más.") }
                    } else {
                        updateActividad(event.actividadId) { it.copy(snCensado = 1) }
                    }
                }
            }
            CensoEvent.Submit -> _uiState.update { it.copy(showSendConfirmationDialog = true) }
            CensoEvent.DismissDialog -> _uiState.update { it.copy(showSendConfirmationDialog = false) }
            CensoEvent.ConfirmSubmission -> submitCensoData()
            CensoEvent.ErrorMessageShown -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val productorInfo = repository.getProductorInfo().first()
            _uiState.update { it.copy(productorInfo = productorInfo) }

            productorInfo?.idProductor?.let { idProductor ->
                repository.getActividadesDesarrollo(ActividadesDesarrolloRequest(idProductor = idProductor, idCategoria = 9000))
                    .onSuccess { response ->
                        val filteredActividades = response.actividades.filter { it.snActivo != 0 }
                        _uiState.update { it.copy(actividades = filteredActividades, isLoading = false) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(error = error.message, isLoading = false) }
                        Log.e("CensoViewModel", "Error fetching data: ${error.message}", error)
                    }
            } ?: _uiState.update { it.copy(isLoading = false, error = "No se pudo obtener la información del productor.") }
        }
    }

    private fun toggleItemSelection(itemId: Int) {
        _uiState.update {
            it.copy(selectedItemId = if (it.selectedItemId == itemId) null else itemId)
        }
    }

    private fun updateActividad(actividadId: Int, updater: (com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadDesarrolloItem) -> com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadDesarrolloItem) {
        _uiState.update { state ->
            val updatedActividades = state.actividades.map {
                if (it.id == actividadId) updater(it) else it
            }
            state.copy(actividades = updatedActividades)
        }
    }

    private fun submitCensoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, showSendConfirmationDialog = false) }
            try {
                val userId = repository.getUserId().first()
                val productorInfo = _uiState.value.productorInfo
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val currentDateTime = sdf.format(Date())
                val radicacion = repository.getSelectedRadicacion().first()

                val actividadesCensadas = _uiState.value.actividades.filter { it.snCensado == 1 }.map {
                    it.copy(
                        telefonoCensoProductor = productorInfo?.productorPhone,
                        censoIdUserCensista = userId,
                        snCensoProductorTelefonoVerificado = if (productorInfo?.phoneVerification == true) 1 else 0,
                        censoFechaRegistroApp = currentDateTime,
                        idCensaEnRadicacion = radicacion?.id
                    )
                }

                if (actividadesCensadas.isNotEmpty()) {
                    val gson = Gson()
                    val jsonActividades = gson.toJson(actividadesCensadas)
                    Log.d("CensoViewModel", "Enviando actividades: $jsonActividades")
                    val request = ActividadesRequest(actividades = jsonActividades)
                    repository.actualizarActividadesProductor(request)
                        .onSuccess {
                            repository.clearProducerInfo()
                            _navigationEvent.send(NavigationEvent.NavigateToDni)
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(error = "Error al actualizar actividades: ${error.message}") }
                        }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Excepción al actualizar actividades: ${e.message}") }
                Log.e("CensoViewModel", "Excepción al actualizar actividades: ${e.message}", e)
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    sealed class NavigationEvent {
        object NavigateToDni : NavigationEvent()
    }
}
