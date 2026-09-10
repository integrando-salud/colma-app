package com.colma.colmaApp.features.MenuMain.viewmodel

import com.colma.colmaApp.features.MenuMain.domain.MenuMainRepository
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.testutils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuMainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MenuMainRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        coEvery { repository.getUserId() } returns 1
        coEvery { repository.getUserName() } returns "Carlos"
        coEvery { repository.getPendingFutureMetasCount() } returns 2
        coEvery { repository.synchronizeData() } returns Result.success("Sincronización exitosa")
    }

    @Test
    fun `cargar datos iniciales obtiene metas pendientes y muestra dialogo si hay meta vencida`() = runTest {
        val pendingMeta = RegistrosSaludModelos.MetaRecord(
            id_meta = 5L,
            id_usuario = 1,
            semana = 1,
            sesion = 1,
            tipo_actividad = "Caminata",
            actividad = "Caminar 20 min",
            lugar = "Plaza",
            acompañante = "Solo",
            fecha_regisitro = "2026-09-01",
            fecha_edicion = null,
            fecha_sincronizacion = null,
            meta_cumplida = 0,
            fecha_objetivo = "2026-09-08",
            motivo_incumplimiento = null
        )

        coEvery { repository.getPendingPastMeta() } returns pendingMeta

        val viewModel = MenuMainViewModel(repository)
        viewModel.onEvent(MenuMainEvent.LoadInitialData)
        advanceUntilIdle()

        assertEquals("Carlos", viewModel.uiState.value.userName)
        assertEquals(2, viewModel.uiState.value.pendingMetasCount)
        assertEquals(pendingMeta, viewModel.uiState.value.todaysObjective)
        assertTrue(viewModel.uiState.value.showObjectiveDialog)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `marcar meta cumplida actualiza el registro con estado cumplido y muestra felicitacion`() = runTest {
        val pendingMeta = RegistrosSaludModelos.MetaRecord(
            id_meta = 5L,
            id_usuario = 1,
            semana = 1,
            sesion = 1,
            tipo_actividad = "Caminata",
            actividad = "Caminar 20 min",
            lugar = "Plaza",
            acompañante = "Solo",
            fecha_regisitro = "2026-09-01",
            fecha_edicion = null,
            fecha_sincronizacion = null,
            meta_cumplida = 0,
            fecha_objetivo = "2026-09-08",
            motivo_incumplimiento = null
        )

        coEvery { repository.getPendingPastMeta() } returns pendingMeta

        val viewModel = MenuMainViewModel(repository)
        viewModel.onEvent(MenuMainEvent.LoadInitialData)
        advanceUntilIdle()

        viewModel.onEvent(MenuMainEvent.ObjectiveCompleted)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateMeta(match { it.meta_cumplida == 2 })
        }
        assertFalse(viewModel.uiState.value.showObjectiveDialog)
        assertTrue(viewModel.uiState.value.showFeedbackDialog)
    }

    @Test
    fun `marcar meta no cumplida con motivo estandar actualiza el registro y muestra mensaje motivacional`() = runTest {
        val pendingMeta = RegistrosSaludModelos.MetaRecord(
            id_meta = 5L,
            id_usuario = 1,
            semana = 1,
            sesion = 1,
            tipo_actividad = "Caminata",
            actividad = "Caminar 20 min",
            lugar = "Plaza",
            acompañante = "Solo",
            fecha_regisitro = "2026-09-01",
            fecha_edicion = null,
            fecha_sincronizacion = null,
            meta_cumplida = 0,
            fecha_objetivo = "2026-09-08",
            motivo_incumplimiento = null
        )

        coEvery { repository.getPendingPastMeta() } returns pendingMeta

        val viewModel = MenuMainViewModel(repository)
        viewModel.onEvent(MenuMainEvent.LoadInitialData)
        advanceUntilIdle()

        viewModel.onEvent(MenuMainEvent.ObjectiveNotCompleted("No tenía energía / ánimo para hacerlo"))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateMeta(match { it.meta_cumplida == 1 && it.motivo_incumplimiento == "No tenía energía / ánimo para hacerlo" })
        }
        assertTrue(viewModel.uiState.value.showFeedbackDialog)
        assertTrue(viewModel.uiState.value.feedbackMessage.contains("energía"))
    }
}
