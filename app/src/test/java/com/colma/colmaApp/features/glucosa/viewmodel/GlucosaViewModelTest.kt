package com.colma.colmaApp.features.glucosa.viewmodel

import com.colma.colmaApp.features.glucosa.domain.GlucosaRepository
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
class GlucosaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: GlucosaRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        coEvery { repository.getHistory() } returns emptyList()
        coEvery { repository.getUserPaisId() } returns 1
    }

    @Test
    fun `la carga inicial obtiene el historial y el pais del usuario`() = runTest {
        val mockHistory = listOf(
            HealthEntry(
                id = 1,
                label = "Glucosa",
                value = "110 mg/dL",
                timestamp = 1000L,
                rawValue1 = 110.0,
                isFasting = true
            )
        )
        coEvery { repository.getHistory() } returns mockHistory
        coEvery { repository.getUserPaisId() } returns 2

        val viewModel = GlucosaViewModel(repository)

        assertEquals(mockHistory, viewModel.uiState.value.history)
        assertEquals(1, viewModel.uiState.value.history.size)
        assertEquals(2, viewModel.uiState.value.userPaisId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `la validacion falla cuando no se selecciona el estado de ayuno`() = runTest {
        val viewModel = GlucosaViewModel(repository)

        viewModel.onEvent(GlucosaEvent.AddGlucosaClicked)
        viewModel.onEvent(GlucosaEvent.GlucosaValueChanged("110"))
        // fasting no seleccionado
        viewModel.onEvent(GlucosaEvent.SaveGlucosaClicked)

        assertEquals("Seleccione si está en ayuno o no", viewModel.uiState.value.formError)
    }

    @Test
    fun `la validacion falla cuando el valor de glucosa tiene menos de 2 caracteres`() = runTest {
        val viewModel = GlucosaViewModel(repository)

        viewModel.onEvent(GlucosaEvent.AddGlucosaClicked)
        viewModel.onEvent(GlucosaEvent.GlucosaValueChanged("5"))
        viewModel.onEvent(GlucosaEvent.FastingChanged(true))
        viewModel.onEvent(GlucosaEvent.SaveGlucosaClicked)

        assertEquals("Ingrese al menos 2 caracteres", viewModel.uiState.value.formError)
    }

    @Test
    fun `glucosa en ayunas menor a 70 clasifica la advertencia como Baja`() = runTest {
        val expectedEntry = HealthEntry(
            id = 10,
            label = "Glucosa",
            value = "65 mg/dL",
            timestamp = System.currentTimeMillis(),
            rawValue1 = 65.0,
            isFasting = true
        )
        coEvery {
            repository.saveGlucosa(
                value = 65.0,
                isFasting = true,
                dateTime = any(),
                advertencia = "Baja"
            )
        } returns Result.success(expectedEntry)

        val viewModel = GlucosaViewModel(repository)

        viewModel.onEvent(GlucosaEvent.AddGlucosaClicked)
        viewModel.onEvent(GlucosaEvent.GlucosaValueChanged("65"))
        viewModel.onEvent(GlucosaEvent.FastingChanged(true))
        viewModel.onEvent(GlucosaEvent.SaveGlucosaClicked)

        coVerify(exactly = 1) {
            repository.saveGlucosa(
                value = 65.0,
                isFasting = true,
                dateTime = any(),
                advertencia = "Baja"
            )
        }
        assertTrue(viewModel.uiState.value.warningDialog.show)
    }

    @Test
    fun `glucosa en ayunas igual o mayor a 126 clasifica la advertencia como Alta`() = runTest {
        val expectedEntry = HealthEntry(
            id = 11,
            label = "Glucosa",
            value = "130 mg/dL",
            timestamp = System.currentTimeMillis(),
            rawValue1 = 130.0,
            isFasting = true
        )
        coEvery {
            repository.saveGlucosa(
                value = 130.0,
                isFasting = true,
                dateTime = any(),
                advertencia = "Alta"
            )
        } returns Result.success(expectedEntry)

        val viewModel = GlucosaViewModel(repository)

        viewModel.onEvent(GlucosaEvent.AddGlucosaClicked)
        viewModel.onEvent(GlucosaEvent.GlucosaValueChanged("130"))
        viewModel.onEvent(GlucosaEvent.FastingChanged(true))
        viewModel.onEvent(GlucosaEvent.SaveGlucosaClicked)

        coVerify(exactly = 1) {
            repository.saveGlucosa(
                value = 130.0,
                isFasting = true,
                dateTime = any(),
                advertencia = "Alta"
            )
        }
        assertTrue(viewModel.uiState.value.showSuccessDialog)
    }

    @Test
    fun `glucosa normal sin ayuno guarda el registro y actualiza el historial`() = runTest {
        val expectedEntry = HealthEntry(
            id = 12,
            label = "Glucosa",
            value = "140 mg/dL",
            timestamp = System.currentTimeMillis(),
            rawValue1 = 140.0,
            isFasting = false
        )
        coEvery {
            repository.saveGlucosa(
                value = 140.0,
                isFasting = false,
                dateTime = any(),
                advertencia = "Normal"
            )
        } returns Result.success(expectedEntry)

        val viewModel = GlucosaViewModel(repository)

        viewModel.onEvent(GlucosaEvent.AddGlucosaClicked)
        viewModel.onEvent(GlucosaEvent.GlucosaValueChanged("140"))
        viewModel.onEvent(GlucosaEvent.FastingChanged(false))
        viewModel.onEvent(GlucosaEvent.SaveGlucosaClicked)

        coVerify(exactly = 1) {
            repository.saveGlucosa(
                value = 140.0,
                isFasting = false,
                dateTime = any(),
                advertencia = "Normal"
            )
        }
        assertNull(viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.isFormVisible)
        assertTrue(viewModel.uiState.value.showSuccessDialog)
    }
}
