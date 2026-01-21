package com.colma.colmaApp.features.sesiones.SesionesMenu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SesionesRepository

@Suppress("UNCHECKED_CAST")
class SesionesViewModelFactory(private val repository: SesionesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SesionesViewModel::class.java)) {
            return SesionesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
