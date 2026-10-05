package com.colma.colmaApp.features.mood.viewmodel

import app.cash.turbine.test
import com.colma.colmaApp.features.mood.domain.MoodRepository
import com.colma.colmaApp.testutils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoodQuizViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MoodRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        coEvery { repository.getUserPaisId() } returns 1
        coEvery { repository.saveMoodRecord(any(), any()) } returns Result.success(100L)
    }

    @Test
    fun `el estado inicial contiene 8 preguntas y respuestas inicializadas en -1`() = runTest {
        val viewModel = MoodQuizViewModel(repository)

        assertEquals(8, viewModel.uiState.value.questions.size)
        assertEquals(8, viewModel.uiState.value.answers.size)
        assertTrue(viewModel.uiState.value.answers.all { it == -1 })
    }

    @Test
    fun `cuando no se responden las dos primeras preguntas, clic en Guardar muestra advertencia`() = runTest {
        val viewModel = MoodQuizViewModel(repository)

        viewModel.onEvent(MoodQuizEvent.SaveClicked)

        assertTrue(viewModel.uiState.value.showWarningDialog)
        assertEquals("Por favor responda las dos primeras preguntas", viewModel.uiState.value.warningDialogMessage)
    }

    @Test
    fun `cuando la suma de las 2 primeras preguntas es menor a 3, se muestra confirmacion para envio parcial`() = runTest {
        val viewModel = MoodQuizViewModel(repository)

        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(0, 1))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(1, 0)) // suma = 1 (< 3)
        viewModel.onEvent(MoodQuizEvent.SaveClicked)

        assertTrue(viewModel.uiState.value.showConfirmDialog)
        assertFalse(viewModel.uiState.value.showMoreQuestionsDialog)
    }

    @Test
    fun `cuando la suma de las 2 primeras preguntas es 3 o mas, se activa el dialogo de preguntas adicionales`() = runTest {
        val viewModel = MoodQuizViewModel(repository)

        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(0, 2))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(1, 1)) // suma = 3 (>= 3)
        viewModel.onEvent(MoodQuizEvent.SaveClicked)

        assertTrue(viewModel.uiState.value.showMoreQuestionsDialog)

        // El usuario acepta continuar respondiendo preguntas adicionales
        viewModel.onEvent(MoodQuizEvent.ConfirmMoreQuestions)
        assertTrue(viewModel.uiState.value.showRestOfQuestions)
        assertTrue(viewModel.uiState.value.firstTwoQuestionsLocked)
    }

    @Test
    fun `al enviar el cuestionario se guardan las respuestas y se emite la navegacion de retorno`() = runTest {
        val viewModel = MoodQuizViewModel(repository)

        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(0, 0))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(1, 1)) // suma = 1 (< 3)
        viewModel.onEvent(MoodQuizEvent.SaveClicked)
        viewModel.onEvent(MoodQuizEvent.ConfirmSubmit)

        coVerify(exactly = 1) { repository.saveMoodRecord(any(), any()) }
        assertTrue(viewModel.uiState.value.showCompletionDialog)
        assertTrue(viewModel.uiState.value.shouldNavigateAfterCompletion)

        viewModel.sideEffects.test {
            viewModel.onEvent(MoodQuizEvent.AcceptCompletionDialog)
            val effect = awaitItem()
            assertEquals(MoodQuizViewModel.SideEffect.NavigateBackWithResult, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `al completar cuestionario con puntaje 0 a 4 se muestra el mensaje de bienestar correspondiente`() = runTest {
        val viewModel = MoodQuizViewModel(repository)

        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(0, 1))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(1, 1)) // suma = 2 (rango 0..4)
        viewModel.onEvent(MoodQuizEvent.SaveClicked)
        viewModel.onEvent(MoodQuizEvent.ConfirmSubmit)

        assertTrue(viewModel.uiState.value.showCompletionDialog)
        val expectedMessage = "Tus respuestas sugieren que no estarías experimentando malestar emocional importante en este momento. Te animamos a seguir cuidándote. Dormir bien, hacer actividad física y pasar tiempo con personas queridas puede ayudarte a mantener tu bienestar."
        assertEquals(expectedMessage, viewModel.uiState.value.completionDialogMessage)
    }

    @Test
    fun `al completar cuestionario con puntaje 5 a 9 se muestra el mensaje correspondiente para Argentina`() = runTest {
        coEvery { repository.getUserPaisId() } returns 1
        val viewModel = MoodQuizViewModel(repository)

        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(0, 2))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(1, 1)) // suma = 3 (>= 3, despliega resto)
        viewModel.onEvent(MoodQuizEvent.SaveClicked)
        viewModel.onEvent(MoodQuizEvent.ConfirmMoreQuestions)

        // Responder resto de preguntas con suma total entre 5 y 9 (3 iniciales + 3 en preguntas 2, 3, 4 y 0 en las demas = 6)
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(2, 1))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(3, 1))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(4, 1))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(5, 0))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(6, 0))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(7, 0)) // total = 6 (rango 5..9)

        viewModel.onEvent(MoodQuizEvent.SaveClicked)
        viewModel.onEvent(MoodQuizEvent.ConfirmSubmit)

        assertTrue(viewModel.uiState.value.showCompletionDialog)
        val expectedMessage = "Tus respuestas sugieren que podés estar sintiendo un leve malestar emocional. \n\n¡Hacer actividad física, darte descansos en la medida de lo posible, y pasar tiempo con personas queridas son cosas que pueden ayudarte!"
        assertEquals(expectedMessage, viewModel.uiState.value.completionDialogMessage)
    }

    @Test
    fun `al completar cuestionario con puntaje 5 a 9 se muestra el mensaje correspondiente para Peru`() = runTest {
        coEvery { repository.getUserPaisId() } returns 2
        val viewModel = MoodQuizViewModel(repository)

        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(0, 2))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(1, 1)) // suma = 3
        viewModel.onEvent(MoodQuizEvent.SaveClicked)
        viewModel.onEvent(MoodQuizEvent.ConfirmMoreQuestions)

        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(2, 1))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(3, 1))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(4, 1))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(5, 0))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(6, 0))
        viewModel.onEvent(MoodQuizEvent.AnswerQuestion(7, 0)) // total = 6 (rango 5..9)

        viewModel.onEvent(MoodQuizEvent.SaveClicked)
        viewModel.onEvent(MoodQuizEvent.ConfirmSubmit)

        assertTrue(viewModel.uiState.value.showCompletionDialog)
        val expectedMessage = "Tus respuestas sugieren que puedes estar sintiendo un leve malestar emocional. \n\n¡Hacer actividad física, darte descansos en la medida de lo posible, y pasar tiempo con personas queridas son cosas que pueden ayudarte!"
        assertEquals(expectedMessage, viewModel.uiState.value.completionDialogMessage)
    }
}
