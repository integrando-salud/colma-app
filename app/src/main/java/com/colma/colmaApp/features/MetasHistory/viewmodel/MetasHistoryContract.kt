package com.colma.colmaApp.features.MetasHistory.viewmodel

import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos

data class MetasHistoryUiState(
    val metas: List<RegistrosSaludModelos.MetaRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class MetasHistoryEvent
