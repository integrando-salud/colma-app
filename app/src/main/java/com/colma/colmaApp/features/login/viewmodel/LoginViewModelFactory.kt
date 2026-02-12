package com.colma.colmaApp.features.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.login.data.LoginRepositoryImpl // Import LoginRepositoryImpl
import com.colma.colmaApp.features.login.domain.LoginRepository
import com.colma.colmaApp.servicios.internetCommunication.LoginService // Import LoginService

@Suppress("UNCHECKED_CAST")
class LoginViewModelFactory() : ViewModelProvider.Factory { // Change constructor to accept Context
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val loginService = LoginService() // Instantiate LoginService
            val loginRepository = LoginRepositoryImpl(loginService) // Instantiate LoginRepositoryImpl
            return LoginViewModel(loginRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
