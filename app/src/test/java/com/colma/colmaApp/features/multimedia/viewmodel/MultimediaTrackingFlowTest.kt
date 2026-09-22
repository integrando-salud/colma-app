package com.colma.colmaApp.features.multimedia.viewmodel

import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.SessionStepsRepository
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord
import com.colma.colmaApp.servicios.Modelos.RegistrosSaludModelos
import com.colma.colmaApp.servicios.internetCommunication.model.MultimediaReadData
import com.colma.colmaApp.testutils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MultimediaTrackingFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: SessionStepsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        coEvery { repository.saveQuizResponses(any(), any()) } returns Result.success(1L)
        coEvery { repository.syncQuizResponse(any(), any()) } returns Result.success("OK")
    }

    @Test
    fun `verificar que un item con tratamiento_multimedia_id se guarda con su id correspondiente`() = runTest {
        val expectedMultimediaId = 565
        val videoOption = MultimediaOption(
            type = MultimediaContentType.VIDEO,
            title = "Video 1",
            description = "Video explicativo",
            tratamientoMultimediaId = expectedMultimediaId,
            isCompleted = false
        )

        coEvery {
            repository.getMultimediaOptions(any(), any(), any())
        } returns Result.success(listOf(videoOption))

        val viewModel = MultimediaViewModel(repository)
        viewModel.onEvent(
            MultimediaEvent.LoadMultimedia(
                sessionId = 1L,
                sessionTitle = "Semana 1",
                subSessionId = 1L,
                subSessionTitle = "Sesión 1",
                treatmentTypesString = "HTA",
                startIndex = 0
            )
        )
        advanceUntilIdle()

        val capturedRecord = slot<RegistrosSaludModelos.MultimediaReadRecord>()
        coVerify { repository.saveMultimediaRead(capture(capturedRecord)) }

        val saved = capturedRecord.captured
        println("[TEST] Video guardado:")
        println("  - Tipo: ${saved.multimedia_type}")
        println("  - tratamiento_multimedia_id: ${saved.tratamiento_multimedia_id}")

        assertNotNull("tratamiento_multimedia_id no debe ser nulo para el video", saved.tratamiento_multimedia_id)
        assertEquals(expectedMultimediaId, saved.tratamiento_multimedia_id)
    }

    @Test
    fun `verificar el comportamiento con items locales que no tienen tratamiento_multimedia_id`() = runTest {
        val descriptionOption = MultimediaOption(
            type = MultimediaContentType.DESCRIPTION,
            title = "Bienvenida",
            description = "Bienvenida a la sesión",
            tratamientoMultimediaId = null, // Local UI step
            isCompleted = false
        )

        coEvery {
            repository.getMultimediaOptions(any(), any(), any())
        } returns Result.success(listOf(descriptionOption))

        val viewModel = MultimediaViewModel(repository)
        viewModel.onEvent(
            MultimediaEvent.LoadMultimedia(
                sessionId = 1L,
                sessionTitle = "Semana 1",
                subSessionId = 1L,
                subSessionTitle = "Sesión 1",
                treatmentTypesString = "HTA",
                startIndex = 0
            )
        )
        advanceUntilIdle()

        val capturedRecord = slot<RegistrosSaludModelos.MultimediaReadRecord>()
        coVerify { repository.saveMultimediaRead(capture(capturedRecord)) }

        val saved = capturedRecord.captured
        println("[TEST] Bienvenida guardada:")
        println("  - Tipo: ${saved.multimedia_type}")
        println("  - tratamiento_multimedia_id: ${saved.tratamiento_multimedia_id}")

        assertNull("Los pasos locales de UI como Bienvenida tienen id nulo", saved.tratamiento_multimedia_id)
    }

    @Test
    fun `simular mapeo a payload de sincronizacion y detectar items con id nulo`() {
        val recordsInDb = listOf(
            RegistrosSaludModelos.MultimediaReadRecord(
                id_multimedia_read = 1,
                id_usuario = 70,
                treatment_id = "DBT_HTA",
                week_id = 1,
                session_id = 1,
                multimedia_type = "DESCRIPTION", // Bienvenida (UI local)
                multimedia_unique_key = "DBT_HTA_1_1_DESCRIPTION_Bienvenida",
                fecha_registro = "2026-09-12 10:00:00",
                fecha_sincronizacion = null,
                tratamiento_multimedia_id = null
            ),
            RegistrosSaludModelos.MultimediaReadRecord(
                id_multimedia_read = 2,
                id_usuario = 70,
                treatment_id = "DBT_HTA",
                week_id = 1,
                session_id = 1,
                multimedia_type = "VIDEO", // Video con ID del servidor
                multimedia_unique_key = "DBT_HTA_1_1_VIDEO_video-1.mp4",
                fecha_registro = "2026-09-12 10:01:00",
                fecha_sincronizacion = null,
                tratamiento_multimedia_id = 565
            ),
            RegistrosSaludModelos.MultimediaReadRecord(
                id_multimedia_read = 3,
                id_usuario = 70,
                treatment_id = "DBT_HTA",
                week_id = 1,
                session_id = 1,
                multimedia_type = "METAS", // Metas
                multimedia_unique_key = "DBT_HTA_1_1_METAS_Metas",
                fecha_registro = "2026-09-12 10:02:00",
                fecha_sincronizacion = null,
                tratamiento_multimedia_id = null
            )
        )

        val syncPayload = recordsInDb.map { mr ->
            MultimediaReadData(
                idMultimediaRead = mr.id_multimedia_read.toInt(),
                idUsuario = mr.id_usuario ?: 70,
                treatmentId = mr.treatment_id ?: "",
                weekId = mr.week_id ?: 0,
                sessionId = mr.session_id ?: 0,
                multimediaType = mr.multimedia_type ?: "",
                multimediaUniqueKey = mr.multimedia_unique_key ?: "",
                fechaRegistro = mr.fecha_registro ?: "2026-09-12 10:00:00",
                fechaSincronizacion = "2026-09-12 10:00:00",
                tratamientoMultimediaId = mr.tratamiento_multimedia_id
            )
        }

        val nullIdItems = syncPayload.filter { it.tratamientoMultimediaId == null }
        val validIdItems = syncPayload.filter { it.tratamientoMultimediaId != null }

        println("[TEST] Total items en multimedia_read_table: ${syncPayload.size}")
        println("[TEST] Items con tratamiento_multimedia_id no nulo: ${validIdItems.size}")
        println("[TEST] Items con tratamiento_multimedia_id NULO: ${nullIdItems.size}")
        nullIdItems.forEach {
            println("  -> Item nulo: tipo=${it.multimediaType}, key=${it.multimediaUniqueKey}")
        }

        assertTrue("El video tiene su ID 565", validIdItems.any { it.tratamientoMultimediaId == 565 })
        assertTrue("Los pasos locales de UI generan items con ID nulo", nullIdItems.isNotEmpty())
    }
}
