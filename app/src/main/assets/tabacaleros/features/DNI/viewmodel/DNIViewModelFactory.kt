package com.codeco.tabacaleros.features.DNI.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codeco.tabacaleros.features.DNI.domain.DNIRepository
import com.codeco.tabacaleros.servicios.SessionManager

@Suppress("UNCHECKED_CAST")
class DNIViewModelFactory(
    private val dniRepository: DNIRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DNIViewModel::class.java)) {
            return DNIViewModel(dniRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
