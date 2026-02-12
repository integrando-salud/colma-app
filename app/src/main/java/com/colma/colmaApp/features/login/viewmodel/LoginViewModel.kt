package com.colma.colmaApp.features.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.login.domain.LoginRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        checkIfLoggedIn() // Disabled for now, as per user request
    }

    private fun checkIfLoggedIn() {
        viewModelScope.launch {
            if (loginRepository.isLoggedIn()) {
                _navigationEvent.send(NavigationEvent.NavigateToHome) // Disabled for now, as per user request
            }
        }
    }

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.DniChanged -> {
                _uiState.update { it.copy(dni = event.dni) }
            }
            is LoginEvent.PasswordChanged -> {
                _uiState.update { it.copy(password = event.password) }
            }
            LoginEvent.LoginClicked -> {
                login()
            }
        }
    }

    private fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = loginRepository.login(_uiState.value.dni, _uiState.value.password)

            result.onSuccess {
                loginRepository.saveSession()
                _navigationEvent.send(NavigationEvent.NavigateToHome) // Re-enabled navigation after manual login
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    sealed class NavigationEvent {
        object NavigateToHome : NavigationEvent()
    }
}