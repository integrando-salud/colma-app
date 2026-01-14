package com.codeco.tabacaleros.features.casaColono.viewmodel

import com.codeco.tabacaleros.servicios.internetCommunication.model.Radicacion

data class CasaColonoUiState(
    val isLoading: Boolean = false,
    val radicaciones: List<Radicacion> = emptyList(),
    val error: String? = null
)

sealed class CasaColonoEvent {
    data class OnRadicacionClicked(val radicacion: Radicacion) : CasaColonoEvent()
}
