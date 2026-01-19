package com.colma.colmaApp.features.RegistroSalud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.RegistroSalud.domain.RegistroSaludRepository

@Suppress("UNCHECKED_CAST")
class RegistroSaludViewModelFactory(private val repository: RegistroSaludRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistroSaludViewModel::class.java)) {
            return RegistroSaludViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
