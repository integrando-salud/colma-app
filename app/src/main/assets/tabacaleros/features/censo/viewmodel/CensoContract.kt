package com.codeco.tabacaleros.features.censo.viewmodel

import com.codeco.tabacaleros.features.DNI.model.ProductorInfo
import com.codeco.tabacaleros.servicios.internetCommunication.model.ActividadDesarrolloItem

data class CensoUiState(
    val productorInfo: ProductorInfo? = null,
    val actividades: List<ActividadDesarrolloItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val selectedItemId: Int? = null,
    val showSendConfirmationDialog: Boolean = false,
)

sealed class CensoEvent {
    object LoadData : CensoEvent()
    data class ToggleItemSelection(val itemId: Int) : CensoEvent()
    data class UpdatePlantas(val actividadId: Int, val plantas: Int) : CensoEvent()
    data class UpdateCuadros(val actividadId: Int, val cuadros: Int) : CensoEvent()
    data class UpdateKilos(val actividadId: Int, val kilos: Int) : CensoEvent()
    data class MarkAsCensado(val actividadId: Int) : CensoEvent()
    object Submit : CensoEvent()
    object DismissDialog : CensoEvent()
    object ConfirmSubmission : CensoEvent()
    object ErrorMessageShown : CensoEvent()
}
