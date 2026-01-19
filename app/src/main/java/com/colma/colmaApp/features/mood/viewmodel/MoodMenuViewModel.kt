package com.colma.colmaApp.features.mood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.mood.domain.MoodRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoodMenuViewModel(private val repository: MoodRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MoodMenuUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()


    init {
        loadHistory()
    }

    fun onEvent(event: MoodMenuEvent) {
        when (event) {
            MoodMenuEvent.RefreshHistory -> loadHistory()
            MoodMenuEvent.AddMoodClicked -> {
                viewModelScope.launch{
                    _navigationEvent.send(NavigationEvent.NavigateToQuiz)
                }
            }
            is MoodMenuEvent.OnBack -> {
                event.onBack()
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val history = repository.getMoodHistory()
            _uiState.update { it.copy(history = history, isLoading = false) }
        }
    }
    
    sealed class NavigationEvent {
        object NavigateToQuiz : NavigationEvent()
    }
}
