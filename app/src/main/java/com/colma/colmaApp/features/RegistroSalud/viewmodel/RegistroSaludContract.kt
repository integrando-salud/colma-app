package com.colma.colmaApp.features.RegistroSalud.viewmodel

/**
 * Represents the state of the Registro de Salud screen.
 */
data class RegistroSaludUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Defines the events that can be triggered from the UI.
 */
sealed class RegistroSaludEvent {
    object OnGlucosaClicked : RegistroSaludEvent()
    object OnPresionClicked : RegistroSaludEvent()
    object OnEstadoAnimoClicked : RegistroSaludEvent()
    object OnBackClicked: RegistroSaludEvent()
}
