package com.colma.colmaApp.features.mood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.mood.domain.MoodRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private var lastMoodDate: Date? = null
    private var countdownJob: Job? = null


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
        // Cancel any existing countdown
        countdownJob?.cancel()
        
        viewModelScope.launch {
            val lastDateStr = repository.getLastMoodDate()
            
            if (lastDateStr == null) {
                // No previous mood records, allow access
                lastMoodDate = null
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
                    lastMoodDate = null
                    _uiState.update { 
                        it.copy(
                            canAccessQuiz = true,
                            restrictionMessage = ""
                        ) 
                    }
                    return@launch
                }
                
                lastMoodDate = lastDate
                val now = Date()
                val diffMillis = now.time - lastDate.time
                val daysDiff = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

                if (daysDiff >= 28) {
                    // 28 or more days have passed, allow access
                    lastMoodDate = null
                    _uiState.update { 
                        it.copy(
                            canAccessQuiz = true,
                            restrictionMessage = ""
                        ) 
                    }
                } else {
                    // Less than 28 days, restrict access and start countdown
                    updateRestrictionMessage(lastDate)
                    
                    // Start countdown timer to update every second
                    countdownJob = viewModelScope.launch {
                        while (true) {
                            delay(1000) // Update every second
                            updateRestrictionMessage(lastDate)
                            
                            // Check if access should now be allowed
                            val currentState = _uiState.value
                            if (currentState.canAccessQuiz) {
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // If parsing fails, allow access by default
                lastMoodDate = null
                _uiState.update { 
                    it.copy(
                        canAccessQuiz = true,
                        restrictionMessage = ""
                    ) 
                }
            }
        }
    }
    
    private suspend fun updateRestrictionMessage(lastDate: Date) {
        val now = Date()
        val exactDiffMillis = now.time - lastDate.time
        val remainingMillis = TimeUnit.DAYS.toMillis(28) - exactDiffMillis
        
        if (remainingMillis > 0) {
            val days = TimeUnit.MILLISECONDS.toDays(remainingMillis).toInt()
            val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis).toInt() % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis).toInt() % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis).toInt() % 60
            
            val message = if (days > 0) {
                "Podrás acceder al cuestionario en $days días y ${String.format("%02d:%02d:%02d", hours, minutes, seconds)} horas"
            } else {
                "Podrás acceder al cuestionario en ${String.format("%02d:%02d:%02d", hours, minutes, seconds)} horas"
            }
            _uiState.update { 
                it.copy(
                    canAccessQuiz = false,
                    restrictionMessage = message
                ) 
            }
        } else {
            // Time's up, allow access
            lastMoodDate = null
            _uiState.update { 
                it.copy(
                    canAccessQuiz = true,
                    restrictionMessage = ""
                ) 
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
