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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MoodMenuViewModel(private val repository: MoodRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MoodMenuUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())


    init {
        loadHistory()
        checkQuizAccess()
    }

    fun onEvent(event: MoodMenuEvent) {
        when (event) {
            MoodMenuEvent.RefreshHistory -> loadHistory()
            MoodMenuEvent.AddMoodClicked -> {
                if (uiState.value.canAccessQuiz) {
                    viewModelScope.launch{
                        _navigationEvent.send(NavigationEvent.NavigateToQuiz)
                    }
                }
                // If cannot access, do nothing (restriction message is shown in UI)
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

    private fun checkQuizAccess() {
        viewModelScope.launch {
            val lastDateStr = repository.getLastMoodDate()
            
            if (lastDateStr == null) {
                // No previous mood records, allow access
                _uiState.update { 
                    it.copy(
                        canAccessQuiz = true,
                        restrictionMessage = ""
                    ) 
                }
                return@launch
            }

            try {
                val lastDate = dateTimeFormat.parse(lastDateStr) ?: run {
                    _uiState.update { 
                        it.copy(
                            canAccessQuiz = true,
                            restrictionMessage = ""
                        ) 
                    }
                    return@launch
                }
                val now = Date()
                val diffMillis = now.time - lastDate.time
                val daysDiff = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

                if (daysDiff >= 28) {
                    // 28 or more days have passed, allow access
                    _uiState.update { 
                        it.copy(
                            canAccessQuiz = true,
                            restrictionMessage = ""
                        ) 
                    }
                } else {
                    // Less than 28 days, restrict access
                    val daysRemaining = 28 - daysDiff
                    val message = if (daysRemaining == 1) {
                        "Podrás acceder al cuestionario en 1 día"
                    } else {
                        "Podrás acceder al cuestionario en $daysRemaining días"
                    }
                    _uiState.update { 
                        it.copy(
                            canAccessQuiz = false,
                            restrictionMessage = message
                        ) 
                    }
                }
            } catch (e: Exception) {
                // If parsing fails, allow access by default
                _uiState.update { 
                    it.copy(
                        canAccessQuiz = true,
                        restrictionMessage = ""
                    ) 
                }
            }
        }
    }

    fun refreshQuizAccess() {
        checkQuizAccess()
    }

    sealed class NavigationEvent {
        object NavigateToQuiz : NavigationEvent()
    }
}
