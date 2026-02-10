package com.colma.colmaApp.features.sesiones.MultimediaSelector.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.content.data.ContentRepositoryImpl
import com.colma.colmaApp.features.sesiones.MultimediaSelector.data.MultimediaRepositoryImpl
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.MultimediaRepository
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper // Added import

@Suppress("UNCHECKED_CAST")
class MultimediaSelectorViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MultimediaSelectorViewModel::class.java)) {
            val contentRepository = ContentRepositoryImpl(context)
            val dbHelper = DBhelper(context) // Instantiate DBhelper
            val repository = MultimediaRepositoryImpl(contentRepository, context, dbHelper) // Pass dbHelper
            return MultimediaSelectorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
