package com.colma.colmaApp.features.sesiones.SessionList.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.sesiones.SessionSteps.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SessionList.domain.SessionListRepository
import com.colma.colmaApp.features.sesiones.SessionList.domain.SubSessionItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionListViewModel(private val repository: SessionListRepository) : ViewModel() {
    private val TAG = "SessionListViewModel"

    private val _uiState = MutableStateFlow(SessionListUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun onEvent(event: SessionListEvent) {
        when (event) {
            is SessionListEvent.SetTreatmentTypes -> {
                _uiState.update { it.copy(treatmentTypes = event.treatmentTypes) }
                loadSessions()
            }
            is SessionListEvent.ExpandSession -> {
                val currentExpandedId = _uiState.value.expandedSessionId
                val newExpandedId = if (currentExpandedId == event.sessionId) null else event.sessionId
                _uiState.update { it.copy(expandedSessionId = newExpandedId) }
                newExpandedId?.let {
                    if (!_uiState.value.subSessions.containsKey(it)) {
                        loadSubSessions(it)
                    }
                }
            }
            is SessionListEvent.SubSessionClick -> {
                viewModelScope.launch {
                    val session = _uiState.value.htaSessions.find { it.id == event.parentSessionId }
                        ?: _uiState.value.diabetesSessions.find { it.id == event.parentSessionId }

                    session?.let {
                        val weekNumber = session.title.replace(Regex("[^0-9]"), "")
                        val sessionTitle = "Semana $weekNumber"

                        _navigationEvent.send(NavigationEvent.NavigateToSessionSteps(
                            event.parentSessionId,
                            sessionTitle,
                            event.subSessionId,
                            event.title,
                            event.treatmentType.name
                        ))
                    }
                }
            }
            SessionListEvent.BackClicked -> {
                 viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateBack)
                }
            }
            SessionListEvent.Refresh -> {
                loadSessions(showLoading = false)
                _uiState.value.expandedSessionId?.let {
                    loadSubSessions(it)
                }
            }
        }
    }

    private fun loadSessions(showLoading: Boolean = true) {
        viewModelScope.launch {
            Log.d(TAG, "Loading sessions for treatment types: ${_uiState.value.treatmentTypes}")
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true) }
            }
            try {
                val hasHta = _uiState.value.treatmentTypes.contains(TreatmentType.HTA)
                val hasDiabetes = _uiState.value.treatmentTypes.contains(TreatmentType.DIABETES)
                val hasDbtHta = _uiState.value.treatmentTypes.contains(TreatmentType.DBT_HTA)

                Log.d(TAG, "Has HTA: $hasHta, Has Diabetes: $hasDiabetes, Has DBT_HTA: $hasDbtHta")

                val htaSessions = if (hasHta) repository.getHtaSessions() else emptyList()
                val diabetesSessions = if (hasDiabetes) {
                    repository.getDiabetesSessions()
                } else if (hasDbtHta) {
                    repository.getDbtHtaSessions()
                } else {
                    emptyList()
                }

                Log.d(TAG, "Loaded ${htaSessions.size} HTA sessions and ${diabetesSessions.size} Diabetes sessions")

                val subSessionsMap = mutableMapOf<Long, List<SubSessionItem>>()
                val allSessions = htaSessions + diabetesSessions
                allSessions.forEach { session ->
                    val subSessions = when (session.treatmentType) {
                        TreatmentType.HTA -> repository.getHtaSubSessions(session.id)
                        TreatmentType.DIABETES -> repository.getDiabetesSubSessions(session.id)
                        TreatmentType.DBT_HTA -> repository.getDbtHtaSubSessions(session.id)
                    }
                    subSessionsMap[session.id] = subSessions
                }

                _uiState.update {
                    it.copy(
                        htaSessions = htaSessions,
                        diabetesSessions = diabetesSessions,
                        subSessions = subSessionsMap,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sessions: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadSubSessions(sessionId: Long) {
        viewModelScope.launch {
            val session = _uiState.value.htaSessions.find { it.id == sessionId }
                ?: _uiState.value.diabetesSessions.find { it.id == sessionId }

            session?.let {
                try {
                    val subSessions = when (it.treatmentType) {
                        TreatmentType.HTA -> repository.getHtaSubSessions(sessionId)
                        TreatmentType.DIABETES -> repository.getDiabetesSubSessions(sessionId)
                        TreatmentType.DBT_HTA -> repository.getDbtHtaSubSessions(sessionId)
                    }
                    _uiState.update { state ->
                        state.copy(subSessions = state.subSessions + (sessionId to subSessions))
                    }
                } catch (e: Exception) {
                     _uiState.update { state ->
                        state.copy(error = e.message)
                    }
                }
            }
        }
    }

    sealed class NavigationEvent {
        object NavigateBack : NavigationEvent()
        data class NavigateToSessionSteps(
            val parentSessionId: Long,
            val sessionTitle: String,
            val subSessionId: Long,
            val subSessionTitle: String,
            val treatmentType: String
        ) : NavigationEvent()
    }
}
