package com.colma.colmaApp.features.multimedia.viewmodel

import app.cash.turbine.test
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaContentType
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.MultimediaOption
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.SessionStepsRepository
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.servicios.Modelos.QuestionResponseRecord
import com.colma.colmaApp.servicios.Modelos.QuizResponseRecord
import com.colma.colmaApp.testutils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MultimediaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: SessionStepsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        coEvery { repository.saveQuizResponses(any(), any()) } returns Result.success(1L)
        coEvery { repository.syncQuizResponse(any(), any()) } returns Result.success("OK")
    }

    @Test
    fun `cargar multimedia obtiene las opciones y establece el indice inicial`() = runTest {
        val mockOptions = listOf(
            MultimediaOption(
                type = MultimediaContentType.VIDEO,
                title = "Video 1",
                description = "Desc video 1",
                isCompleted = false
            ),
            MultimediaOption(
                type = MultimediaContentType.QUIZ,
                title = "Quiz 1",
                description = "Desc quiz 1",
                isCompleted = false
            )
        )

        coEvery {
            repository.getMultimediaOptions(listOf(TreatmentType.HTA), 1L, 1L)
        } returns Result.success(mockOptions)

        val viewModel = MultimediaViewModel(repository)

        viewModel.onEvent(
            MultimediaEvent.LoadMultimedia(
                sessionId = 1L,
                sessionTitle = "Semana 1",
                subSessionId = 1L,
                subSessionTitle = "Sesion 1",
                treatmentTypesString = "HTA",
                startIndex = 0
            )
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.multimediaOptions.size)
        assertEquals(0, viewModel.uiState.value.currentMultimediaIndex)
        assertEquals("Video 1", viewModel.uiState.value.currentMultimediaOption?.title)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `la navegacion siguiente y anterior actualiza correctamente el indice actual`() = runTest {
        val mockOptions = listOf(
            MultimediaOption(
                type = MultimediaContentType.VIDEO,
                title = "Video 1",
                description = "Desc 1",
                isCompleted = false
            ),
            MultimediaOption(
                type = MultimediaContentType.QUIZ,
                title = "Quiz 1",
                description = "Desc 2",
                isCompleted = false
            )
        )

        coEvery {
            repository.getMultimediaOptions(any(), any(), any())
        } returns Result.success(mockOptions)

        val viewModel = MultimediaViewModel(repository)
        viewModel.onEvent(
            MultimediaEvent.LoadMultimedia(1L, "S1", 1L, "Sub1", "HTA", 0)
        )
        advanceUntilIdle()

        viewModel.onEvent(MultimediaEvent.GoToNext)
        assertEquals(1, viewModel.uiState.value.currentMultimediaIndex)
        assertEquals(MultimediaContentType.QUIZ, viewModel.uiState.value.currentMultimediaOption?.type)

        viewModel.onEvent(MultimediaEvent.GoToPrevious)
        assertEquals(0, viewModel.uiState.value.currentMultimediaIndex)
    }

    @Test
    fun `enviar quiz guarda respuestas, marca lectura en bd e inicia sincronizacion`() = runTest {
        val mockOptions = listOf(
            MultimediaOption(
                type = MultimediaContentType.QUIZ,
                title = "Quiz 1",
                description = "Desc quiz",
                isCompleted = false
            )
        )
        coEvery { repository.getMultimediaOptions(any(), any(), any()) } returns Result.success(mockOptions)

        val viewModel = MultimediaViewModel(repository)
        viewModel.onEvent(
            MultimediaEvent.LoadMultimedia(1L, "S1", 1L, "Sub1", "HTA", 0)
        )
        advanceUntilIdle()

        val quizResponse = QuizResponseRecord(
            id_quiz_response = 0L,
            id_usuario = 1,
            quiz_type = "HTA",
            week_id = 1,
            session_id = 1,
            quiz_title = "Quiz Salud",
            fecha_registro = "2026-09-10 10:00:00",
            fecha_sincronizacion = null,
            total_questions = 1,
            correct_answers = 1,
            percentage_correct = 100.0,
            multimedia_unique_key = "Q1",
            treatment_type = "HTA"
        )

        val questionResponses = listOf(
            QuestionResponseRecord(
                id_question_response = 0L,
                id_quiz_response = 0L,
                question_index = 0,
                question_text = "¿Tomó su medicación?",
                user_answer_index = 0,
                user_answer_text = "Sí",
                correct_answer_index = 0,
                correct_answer_text = "Sí",
                is_correct = 1,
                posibles_respuestas = "[\"Sí\",\"No\"]"
            )
        )

        viewModel.onEvent(MultimediaEvent.QuizSubmitted(quizResponse, questionResponses))

        coVerify(timeout = 3000) { repository.saveQuizResponses(quizResponse, questionResponses) }
        coVerify(timeout = 3000) { repository.saveMultimediaRead(any()) }
    }

    @Test
    fun `evento finalizar emite la accion de navegar hacia atras`() = runTest {
        val viewModel = MultimediaViewModel(repository)

        viewModel.navigationEvent.test {
            viewModel.onEvent(MultimediaEvent.Finish)
            val event = awaitItem()
            assertEquals(MultimediaViewModel.NavigationEvent.NavigateBack, event)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
