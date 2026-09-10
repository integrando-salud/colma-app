package com.colma.colmaApp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.colma.colmaApp.utils.varios.HealthEntry
import com.colma.colmaApp.utils.varios.HistoryCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryCardComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `el componente HistoryCard muestra registros de salud y ejecuta el callback al hacer clic`() {
        var clickedEntry: HealthEntry? = null

        val entries = listOf(
            HealthEntry(
                id = 1,
                label = "Presión",
                value = "120/80 mmHg",
                timestamp = System.currentTimeMillis()
            )
        )

        composeTestRule.setContent {
            HistoryCard(
                history = entries,
                onClear = {},
                onEntryClick = { clickedEntry = it }
            )
        }

        // Verificar que la etiqueta y el valor se muestran en pantalla
        composeTestRule.onNodeWithText("Presión").assertIsDisplayed()
        composeTestRule.onNodeWithText("120/80 mmHg").assertIsDisplayed()

        // Realizar clic sobre el registro
        composeTestRule.onNodeWithText("120/80 mmHg").performClick()

        // Verificar que se invocó el callback con el ID correcto
        assertEquals(1L, clickedEntry?.id)
    }
}

