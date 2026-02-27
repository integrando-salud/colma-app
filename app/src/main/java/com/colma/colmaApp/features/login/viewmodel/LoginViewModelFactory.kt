package com.colma.colmaApp.features.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.login.data.LoginRepositoryImpl // Import LoginRepositoryImpl
import com.colma.colmaApp.features.login.domain.LoginRepository
import com.colma.colmaApp.servicios.internetCommunication.LoginService // Import LoginService
import android.content.Context
import com.colma.colmaApp.servicios.database.dbhelper.DBhelper


@Suppress("UNCHECKED_CAST")
class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory { // Change constructor to accept Context
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val loginService = LoginService() // Instantiate LoginService
            val dbHelper = DBhelper(context) // Instantiate DBhelper
            val loginRepository = LoginRepositoryImpl(loginService, dbHelper) // Instantiate LoginRepositoryImpl
            return LoginViewModel(loginRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
