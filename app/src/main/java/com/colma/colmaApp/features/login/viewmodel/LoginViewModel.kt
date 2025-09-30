package com.colma.colmaApp.features.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _navigateToMain = MutableSharedFlow<Unit>(replay = 0)
    val navigateToMain = _navigateToMain.asSharedFlow()

    fun onLoginClick() {
        viewModelScope.launch {
            _navigateToMain.emit(Unit)
        }
    }
}