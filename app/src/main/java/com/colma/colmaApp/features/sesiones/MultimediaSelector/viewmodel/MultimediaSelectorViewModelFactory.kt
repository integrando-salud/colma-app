package com.colma.colmaApp.features.sesiones.MultimediaSelector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaRepository

@Suppress("UNCHECKED_CAST")
class MultimediaSelectorViewModelFactory(
    private val repository: MultimediaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MultimediaSelectorViewModel::class.java)) {
            return MultimediaSelectorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
