package com.codeco.tabacaleros.features.home.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codeco.tabacaleros.features.home.data.HomeRepositoryImpl
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.SubirImagenesProductorService

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val sessionManager = SessionManager(context)
            val subirImagenesProductorService = SubirImagenesProductorService(sessionManager)
            val homeRepository = HomeRepositoryImpl(subirImagenesProductorService, sessionManager)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(homeRepository, sessionManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
