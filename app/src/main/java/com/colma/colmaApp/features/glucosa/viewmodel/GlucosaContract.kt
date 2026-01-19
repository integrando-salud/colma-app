package com.colma.colmaApp.features.glucosa.viewmodel

import com.colma.colmaApp.utils.varios.HealthEntry

data class GlucosaUiState(
    val history: List<HealthEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFormVisible: Boolean = false,
    // Form fields
    val glucosaValue: String = "",
    val isFasting: Boolean? = null,
    val measurementTime: Long = System.currentTimeMillis(),
    val formError: String? = null,
    
    // Details dialog
    val isDetailsDialogVisible: Boolean = false,
    val selectedEntry: HealthEntry? = null,

    val lastGlucosa: HealthEntry? = null
)

sealed class GlucosaEvent {
    object AddGlucosaClicked : GlucosaEvent()
    object DismissFormDialog : GlucosaEvent()
    object SaveGlucosaClicked : GlucosaEvent()
    data class GlucosaValueChanged(val value: String) : GlucosaEvent()
    data class FastingChanged(val isFasting: Boolean) : GlucosaEvent()
    data class DateChanged(val year: Int, val month: Int, val day: Int) : GlucosaEvent()
    data class TimeChanged(val hour: Int, val minute: Int) : GlucosaEvent()
    data class ShowDetails(val entry: HealthEntry): GlucosaEvent()
    object DismissDetailsDialog: GlucosaEvent()
}
