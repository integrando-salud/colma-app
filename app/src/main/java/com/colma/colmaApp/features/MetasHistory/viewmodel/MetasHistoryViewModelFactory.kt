package com.colma.colmaApp.features.MetasHistory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.MetasHistory.domain.MetasHistoryRepository

@Suppress("UNCHECKED_CAST")
class MetasHistoryViewModelFactory(private val repository: MetasHistoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetasHistoryViewModel::class.java)) {
            return MetasHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
