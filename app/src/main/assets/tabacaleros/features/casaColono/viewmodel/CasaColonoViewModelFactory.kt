package com.codeco.tabacaleros.features.casaColono.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codeco.tabacaleros.features.casaColono.domain.CasaColonoRepository
import com.codeco.tabacaleros.servicios.SessionManager

class CasaColonoViewModelFactory(
    private val repository: CasaColonoRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CasaColonoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CasaColonoViewModel(repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
