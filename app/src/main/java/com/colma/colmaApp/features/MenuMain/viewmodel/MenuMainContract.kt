package com.colma.colmaApp.features.MenuMain.viewmodel

import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos

/**
 * Represents the state of the MenuMain screen.
 */
data class MenuMainUiState(
    val userName: String = "Usuario", // Default name
    val pendingMetasCount: Int = 0,
    val showObjectiveDialog: Boolean = false,
    val todaysObjective: RegistrosSaludModelos.MetaRecord? = null,
    val showReasonDialog: Boolean = false,
    val showOtherReasonDialog: Boolean = false,
    val showFeedbackDialog: Boolean = false,
    val feedbackMessage: String = "",
    val selectedReason: String = "",
    val otherReasonInput: String = "",
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

/**
 * Defines the events that can be triggered from the UI.
 */
sealed class MenuMainEvent {
    object LoadInitialData : MenuMainEvent()
    object OnSessionsClick : MenuMainEvent()
    object OnHealthRegistersClick : MenuMainEvent()
    object OnMetasHistoryClick : MenuMainEvent()
    object OnSalirClick : MenuMainEvent()
    object OnSyncClick : MenuMainEvent()
    object ObjectiveDialogDismissed : MenuMainEvent()
    object ObjectiveCompleted : MenuMainEvent()
    object DismissFeedbackDialog : MenuMainEvent()
    data class ObjectiveNotCompleted(val reason: String) : MenuMainEvent()
    data class OtherReasonChanged(val reason: String) : MenuMainEvent()
    object OtherReasonConfirmed : MenuMainEvent()
    object SnackbarDismissed : MenuMainEvent()
}
