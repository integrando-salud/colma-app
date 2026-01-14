package com.codeco.tabacaleros.features.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codeco.tabacaleros.features.login.data.LoginRepositoryImpl
import com.codeco.tabacaleros.features.login.domain.LoginRepository
import com.codeco.tabacaleros.servicios.SessionManager
import com.codeco.tabacaleros.servicios.internetCommunication.LoginService

@Suppress("UNCHECKED_CAST")
class LoginViewModelFactory(private val loginRepository: LoginRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(loginRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
