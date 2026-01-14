package com.codeco.tabacaleros.features.censo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codeco.tabacaleros.features.censo.domain.CensoRepository

@Suppress("UNCHECKED_CAST")
class CensoViewModelFactory(private val censoRepository: CensoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CensoViewModel::class.java)) {
            return CensoViewModel(censoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
