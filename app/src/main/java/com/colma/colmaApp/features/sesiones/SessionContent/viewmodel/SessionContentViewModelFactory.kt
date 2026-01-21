package com.colma.colmaApp.features.sesiones.SessionContent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.sesiones.SessionContent.domain.SessionContentRepository

@Suppress("UNCHECKED_CAST")
class SessionContentViewModelFactory(private val repository: SessionContentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionContentViewModel::class.java)) {
            return SessionContentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
