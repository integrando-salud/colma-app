package com.codeco.tabacaleros.features.Menu.viewmodel

import com.codeco.tabacaleros.servicios.internetCommunication.model.Component

data class MenuUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val components: List<Component> = emptyList(),
    val radicacionName: String? = null
)

sealed class MenuEvent {
    object OnRegistroClicked : MenuEvent()
    object OnConsultaClicked : MenuEvent()
}
