package com.colma.colmaApp.features.multimedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaRepository

@Suppress("UNCHECKED_CAST")
class MultimediaViewModelFactory(private val multimediaRepository: MultimediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MultimediaViewModel::class.java)) {
            return MultimediaViewModel(multimediaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
