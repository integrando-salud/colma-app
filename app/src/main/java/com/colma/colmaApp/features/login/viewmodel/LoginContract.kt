package com.colma.colmaApp.features.login.viewmodel

/**
 * Type of error occurred during the login process.
 */
enum class LoginErrorType {
    INVALID_CREDENTIALS,
    NO_INTERNET,
    SERVER_ERROR,
    UNEXPECTED
}

/**
 * Represents the state of the Login screen.
 */
data class LoginUiState(
    val dni: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val error: String? = null,
    val errorTitle: String? = null,
    val errorType: LoginErrorType? = null
)

/**
 * Defines the events that can be triggered from the UI.
 */
sealed class LoginEvent {
    data class DniChanged(val dni: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object LoginClicked : LoginEvent()
    object DismissError : LoginEvent()
}

