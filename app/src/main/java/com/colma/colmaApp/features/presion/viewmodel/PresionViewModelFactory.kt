package com.colma.colmaApp.features.presion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.presion.domain.PresionRepository

@Suppress("UNCHECKED_CAST")
class PresionViewModelFactory(private val repository: PresionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PresionViewModel::class.java)) {
            return PresionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
