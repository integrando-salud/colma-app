package com.colma.colmaApp.features.MetasHistory.viewmodel

import com.colma.colmaApp.features.MetasHistory.domain.MetasHistoryRepository
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.testutils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetasHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MetasHistoryRepository = mockk(relaxed = true)

    @Test
    fun `la carga inicial obtiene todas las metas y actualiza el estado`() = runTest {
        val mockMetas = listOf(
            RegistrosSaludModelos.MetaRecord(
                id_meta = 1L,
                id_usuario = 1,
                semana = 1,
                sesion = 1,
                tipo_actividad = "Caminata",
                actividad = "Caminar 30 min",
                lugar = "Parque",
                acompañante = "Familia",
                fecha_regisitro = "2026-09-01",
                fecha_edicion = null,
                fecha_sincronizacion = null,
                meta_cumplida = 1,
                fecha_objetivo = "2026-09-05",
                motivo_incumplimiento = null
            )
        )

        coEvery { repository.getAllMetas(any()) } returns Result.success(mockMetas)

        val viewModel = MetasHistoryViewModel(repository)

        assertEquals(mockMetas, viewModel.uiState.value.metas)
        assertEquals(1, viewModel.uiState.value.metas.size)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `cuando falla la obtencion de metas, se establece el error en el estado`() = runTest {
        val errorMessage = "Error cargando metas"
        coEvery { repository.getAllMetas(any()) } returns Result.failure(Exception(errorMessage))

        val viewModel = MetasHistoryViewModel(repository)

        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
