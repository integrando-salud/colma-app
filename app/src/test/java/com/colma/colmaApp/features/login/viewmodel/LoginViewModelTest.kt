package com.colma.colmaApp.features.login.viewmodel

import app.cash.turbine.test
import com.colma.colmaApp.features.login.domain.LoginRepository
import com.colma.colmaApp.testutils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loginRepository: LoginRepository = mockk(relaxed = true)

    @Test
    fun `cuando el usuario ya inicio sesion, se emite evento para navegar al home`() = runTest {
        coEvery { loginRepository.isLoggedIn() } returns true

        val viewModel = LoginViewModel(loginRepository)

        viewModel.navigationEvent.test {
            advanceTimeBy(3000)
            val event = awaitItem()
            assertTrue(event is LoginViewModel.NavigationEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando el usuario no inicio sesion, la verificacion de sesion concluye en falso`() = runTest {
        coEvery { loginRepository.isLoggedIn() } returns false

        val viewModel = LoginViewModel(loginRepository)

        advanceTimeBy(3000)
        assertFalse(viewModel.uiState.value.isCheckingSession)
    }

    @Test
    fun `cuando cambian el DNI y la clave, se actualiza el estado de la pantalla`() = runTest {
        coEvery { loginRepository.isLoggedIn() } returns false
        val viewModel = LoginViewModel(loginRepository)

        viewModel.onEvent(LoginEvent.DniChanged("12345678"))
        viewModel.onEvent(LoginEvent.PasswordChanged("secret123"))

        assertEquals("12345678", viewModel.uiState.value.dni)
        assertEquals("secret123", viewModel.uiState.value.password)
    }

    @Test
    fun `cuando el login es exitoso, se guarda la sesion y se navega al home`() = runTest {
        coEvery { loginRepository.isLoggedIn() } returns false
        coEvery { loginRepository.login("12345678", "secret123") } returns Result.success(true)

        val viewModel = LoginViewModel(loginRepository)
        advanceTimeBy(3000)

        viewModel.onEvent(LoginEvent.DniChanged("12345678"))
        viewModel.onEvent(LoginEvent.PasswordChanged("secret123"))

        viewModel.navigationEvent.test {
            viewModel.onEvent(LoginEvent.LoginClicked)

            val navEvent = awaitItem()
            assertTrue(navEvent is LoginViewModel.NavigationEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { loginRepository.saveSession() }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `cuando el login falla, se establece el mensaje de error en el estado`() = runTest {
        val errorMessage = "Credenciales incorrectas"
        coEvery { loginRepository.isLoggedIn() } returns false
        coEvery { loginRepository.login("12345678", "wrong_pwd") } returns Result.failure(Exception(errorMessage))

        val viewModel = LoginViewModel(loginRepository)
        advanceTimeBy(3000)

        viewModel.onEvent(LoginEvent.DniChanged("12345678"))
        viewModel.onEvent(LoginEvent.PasswordChanged("wrong_pwd"))
        viewModel.onEvent(LoginEvent.LoginClicked)

        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
