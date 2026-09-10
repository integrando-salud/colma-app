package com.colma.colmaApp.features.presion.viewmodel

import com.colma.colmaApp.features.presion.domain.PresionRepository
import com.colma.colmaApp.testutils.MainDispatcherRule
import com.colma.colmaApp.utils.varios.HealthEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PresionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: PresionRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        coEvery { repository.getHistory() } returns emptyList()
        coEvery { repository.getUserPaisId() } returns 1
        coEvery { repository.checkHighPressureTrend() } returns false
    }

    @Test
    fun `la carga inicial obtiene el historial y el pais del usuario`() = runTest {
        val mockHistory = listOf(
            HealthEntry(
                id = 1,
                label = "Presión",
                value = "120/80 mmHg",
                timestamp = 1000L,
                rawValue1 = 120.0,
                rawValue2 = 80.0
            )
        )
        coEvery { repository.getHistory() } returns mockHistory
        coEvery { repository.getUserPaisId() } returns 2

        val viewModel = PresionViewModel(repository)

        assertEquals(mockHistory, viewModel.uiState.value.history)
        assertEquals(1, viewModel.uiState.value.history.size)
        assertEquals(2, viewModel.uiState.value.userPaisId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `la validacion falla cuando la sistolica es menor o igual a la diastolica`() = runTest {
        val viewModel = PresionViewModel(repository)

        viewModel.onEvent(PresionEvent.AddPresionClicked)
        viewModel.onEvent(PresionEvent.SistolicaChanged("80"))
        viewModel.onEvent(PresionEvent.DiastolicaChanged("120"))
        viewModel.onEvent(PresionEvent.SavePresionClicked)

        assertEquals("El valor mayor debe ser mayor que el menor", viewModel.uiState.value.formError)
    }

    @Test
    fun `la validacion falla cuando la sistolica esta fuera del rango plausible`() = runTest {
        val viewModel = PresionViewModel(repository)

        viewModel.onEvent(PresionEvent.AddPresionClicked)
        viewModel.onEvent(PresionEvent.SistolicaChanged("350"))
        viewModel.onEvent(PresionEvent.DiastolicaChanged("80"))
        viewModel.onEvent(PresionEvent.SavePresionClicked)

        assertTrue(viewModel.uiState.value.formError?.contains("fuera de rango") == true)
    }

    @Test
    fun `la validacion es exitosa y guarda un registro de presion normal`() = runTest {
        val expectedEntry = HealthEntry(
            id = 10,
            label = "Presión",
            value = "120/80 mmHg",
            timestamp = System.currentTimeMillis(),
            rawValue1 = 120.0,
            rawValue2 = 80.0
        )
        coEvery {
            repository.savePresion(
                sistolica = 120.0,
                diastolica = 80.0,
                dateTime = any(),
                advertencia = "Normal"
            )
        } returns Result.success(expectedEntry)

        val viewModel = PresionViewModel(repository)

        viewModel.onEvent(PresionEvent.AddPresionClicked)
        viewModel.onEvent(PresionEvent.SistolicaChanged("120"))
        viewModel.onEvent(PresionEvent.DiastolicaChanged("80"))
        viewModel.onEvent(PresionEvent.SavePresionClicked)

        coVerify(exactly = 1) {
            repository.savePresion(
                sistolica = 120.0,
                diastolica = 80.0,
                dateTime = any(),
                advertencia = "Normal"
            )
        }

        assertNull(viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.isFormVisible)
        assertTrue(viewModel.uiState.value.showSuccessDialog)
    }

    @Test
    fun `guardar registro de presion alta clasifica la advertencia como Alta`() = runTest {
        val expectedEntry = HealthEntry(
            id = 11,
            label = "Presión",
            value = "150/95 mmHg",
            timestamp = System.currentTimeMillis(),
            rawValue1 = 150.0,
            rawValue2 = 95.0
        )
        coEvery {
            repository.savePresion(
                sistolica = 150.0,
                diastolica = 95.0,
                dateTime = any(),
                advertencia = "Alta"
            )
        } returns Result.success(expectedEntry)

        val viewModel = PresionViewModel(repository)

        viewModel.onEvent(PresionEvent.AddPresionClicked)
        viewModel.onEvent(PresionEvent.SistolicaChanged("150"))
        viewModel.onEvent(PresionEvent.DiastolicaChanged("95"))
        viewModel.onEvent(PresionEvent.SavePresionClicked)

        coVerify(exactly = 1) {
            repository.savePresion(
                sistolica = 150.0,
                diastolica = 95.0,
                dateTime = any(),
                advertencia = "Alta"
            )
        }
    }
}
