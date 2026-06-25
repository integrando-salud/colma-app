package com.colma.colmaApp.features.multimedia.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.content.data.ContentRepositoryImpl
import com.colma.colmaApp.features.sesiones.SessionSteps.data.SessionStepsRepositoryImpl
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.SessionStepsRepository
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper

@Suppress("UNCHECKED_CAST")
class MultimediaViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MultimediaViewModel::class.java)) {
            val contentRepository = ContentRepositoryImpl(context)
            val dbHelper = DbHelper(context)
            val repository = SessionStepsRepositoryImpl(contentRepository, context, dbHelper)
            return MultimediaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
