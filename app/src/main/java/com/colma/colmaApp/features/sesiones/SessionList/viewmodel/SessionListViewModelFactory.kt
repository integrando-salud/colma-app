package com.colma.colmaApp.features.sesiones.SessionList.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.content.data.ContentRepositoryImpl
import com.colma.colmaApp.features.sesiones.SessionList.data.SessionListRepositoryImpl
import com.colma.colmaApp.features.sesiones.SessionList.domain.SessionListRepository
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper

@Suppress("UNCHECKED_CAST")
class SessionListViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionListViewModel::class.java)) {
            val contentRepository = ContentRepositoryImpl(context)
            val dbHelper = DbHelper(context)
            val repository = SessionListRepositoryImpl(contentRepository, context, dbHelper)
            return SessionListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
