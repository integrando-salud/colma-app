package com.colma.colmaApp.features.mood.viewmodel

import com.colma.colmaApp.utils.varios.HealthEntry

data class MoodMenuUiState(
    val history: List<HealthEntry> = emptyList(),
    val isLoading: Boolean = false,
    val canAccessQuiz: Boolean = true,
    val restrictionMessage: String = "",
    val restrictionDate: String = ""
)

sealed class MoodMenuEvent {
    object AddMoodClicked : MoodMenuEvent()
    object RefreshHistory : MoodMenuEvent()
    data class OnBack(val onBack: ()-> Unit) : MoodMenuEvent()
}
