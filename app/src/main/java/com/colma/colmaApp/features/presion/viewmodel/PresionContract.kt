package com.colma.colmaApp.features.presion.viewmodel

import com.colma.colmaApp.utils.varios.HealthEntry

data class PresionUiState(
    val history: List<HealthEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFormVisible: Boolean = false,
    val showSuccessDialog: Boolean = false,
    // Form fields
    val sistolica: String = "",
    val diastolica: String = "",
    val measurementTime: Long = System.currentTimeMillis(),
    val formError: String? = null,

    // Details dialog
    val isDetailsDialogVisible: Boolean = false,
    val selectedEntry: HealthEntry? = null,

    val lastPresion: HealthEntry? = null,

    val warningDialog: WarningDialog = WarningDialog()
) {
    data class WarningDialog(
        val show: Boolean = false,
        val message: String = ""
    )
}

sealed class PresionEvent {
    object AddPresionClicked : PresionEvent()
    object DismissFormDialog : PresionEvent()
    object DismissSuccessDialog : PresionEvent()
    object SavePresionClicked : PresionEvent()
    data class SistolicaChanged(val value: String) : PresionEvent()
    data class DiastolicaChanged(val value: String) : PresionEvent()
    data class DateChanged(val year: Int, val month: Int, val day: Int) : PresionEvent()
    data class TimeChanged(val hour: Int, val minute: Int) : PresionEvent()
    data class ShowDetails(val entry: HealthEntry) : PresionEvent()
    object DismissDetailsDialog : PresionEvent()
    object DismissWarningDialog : PresionEvent()
}
