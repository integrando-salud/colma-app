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
        checkIfLoggedIn()
    }

    private fun checkIfLoggedIn() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500) // Artificial delay for smoother transition
            if (loginRepository.isLoggedIn()) {
                _navigationEvent.send(NavigationEvent.NavigateToHome)
            } else {
                _uiState.update { it.copy(isCheckingSession = false) }
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
            LoginEvent.DismissError -> {
                clearError()
            }
        }
    }

    private fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorTitle = null, errorType = null) }

            val result = loginRepository.login(_uiState.value.dni, _uiState.value.password)

            result.onSuccess {
                loginRepository.saveSession()
                _navigationEvent.send(NavigationEvent.NavigateToHome) // Re-enabled navigation after manual login
            }.onFailure { error ->
                val (title, message, errorType) = parseError(error)
                _uiState.update { 
                    it.copy(
                        error = message,
                        errorTitle = title,
                        errorType = errorType
                    ) 
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, errorTitle = null, errorType = null) }
    }

    private fun parseError(throwable: Throwable): Triple<String, String, LoginErrorType> {
        val rawMessage = throwable.message ?: ""
        return when {
            rawMessage.contains("NO_INTERNET", ignoreCase = true) ||
            rawMessage.contains("No tienes conexión", ignoreCase = true) ||
            rawMessage.contains("Sin conexión", ignoreCase = true) ||
            rawMessage.contains("Unable to resolve host", ignoreCase = true) ||
            rawMessage.contains("ConnectException", ignoreCase = true) ||
            rawMessage.contains("UnknownHostException", ignoreCase = true) -> {
                Triple(
                    "Sin conexión a internet",
                    if (rawMessage.contains("primera vez", ignoreCase = true)) {
                        "No se detectó conexión a internet. Para iniciar sesión por primera vez en este dispositivo necesitas conexión a la red."
                    } else {
                        "No se pudo conectar con el servidor. Comprueba tu conexión a internet e inténtalo nuevamente."
                    },
                    LoginErrorType.NO_INTERNET
                )
            }
            rawMessage.contains("INVALID_CREDENTIALS", ignoreCase = true) ||
            rawMessage.contains("Contraseña incorrecta", ignoreCase = true) ||
            rawMessage.contains("credencial", ignoreCase = true) ||
            rawMessage.contains("401") ||
            rawMessage.contains("400") ||
            rawMessage.contains("403") ||
            rawMessage.contains("404") ||
            rawMessage.contains("Credenciales incorrectas", ignoreCase = true) -> {
                Triple(
                    "Credenciales incorrectas",
                    "El usuario o la contraseña ingresados no son correctos. Por favor, verifica tus datos e inténtalo nuevamente.",
                    LoginErrorType.INVALID_CREDENTIALS
                )
            }
            rawMessage.contains("SERVER_ERROR", ignoreCase = true) ||
            rawMessage.contains("500") ||
            rawMessage.contains("502") ||
            rawMessage.contains("503") ||
            rawMessage.contains("504") ||
            rawMessage.contains("servidor", ignoreCase = true) -> {
                Triple(
                    "Servidor no disponible",
                    "El servidor de Colma no se encuentra disponible temporalmente. Por favor, intenta más tarde.",
                    LoginErrorType.SERVER_ERROR
                )
            }
            else -> {
                Triple(
                    "Error al iniciar sesión",
                    if (rawMessage.isNotBlank()) rawMessage else "Ocurrió un error inesperado al intentar iniciar sesión. Por favor, inténtalo nuevamente.",
                    LoginErrorType.UNEXPECTED
                )
            }
        }
    }

    sealed class NavigationEvent {
        object NavigateToHome : NavigationEvent()
    }
}