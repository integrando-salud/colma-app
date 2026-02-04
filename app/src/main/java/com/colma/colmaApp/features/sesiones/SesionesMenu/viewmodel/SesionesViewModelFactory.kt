package com.colma.colmaApp.features.sesiones.SesionesMenu.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.content.data.ContentRepositoryImpl
import com.colma.colmaApp.features.sesiones.SesionesMenu.data.SesionesRepositoryImpl
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SesionesRepository

@Suppress("UNCHECKED_CAST")
class SesionesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SesionesViewModel::class.java)) {
            val contentRepository = ContentRepositoryImpl(context)
            val repository = SesionesRepositoryImpl(contentRepository, context)
            return SesionesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
