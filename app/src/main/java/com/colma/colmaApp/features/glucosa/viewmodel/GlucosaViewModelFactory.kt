package com.colma.colmaApp.features.glucosa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.glucosa.domain.GlucosaRepository

@Suppress("UNCHECKED_CAST")
class GlucosaViewModelFactory(private val repository: GlucosaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlucosaViewModel::class.java)) {
            return GlucosaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
