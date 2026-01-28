package com.colma.colmaApp.features.sesiones.SesionesMenu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.sesiones.MultimediaSelector.domain.TreatmentType
import com.colma.colmaApp.features.sesiones.SesionesMenu.domain.SesionesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SesionesViewModel(private val repository: SesionesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SesionesUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun onEvent(event: SesionesEvent) {
        when (event) {
            is SesionesEvent.SetTreatmentTypes -> {
                _uiState.update { it.copy(treatmentTypes = event.treatmentTypes) }
                loadSessions()
            }
            is SesionesEvent.ExpandSession -> {
                val currentExpandedId = _uiState.value.expandedSessionId
                val newExpandedId = if (currentExpandedId == event.sessionId) null else event.sessionId
                _uiState.update { it.copy(expandedSessionId = newExpandedId) }
                newExpandedId?.let {
                    if (!_uiState.value.subSessions.containsKey(it)) {
                        loadSubSessions(it)
                    }
                }
            }
            is SesionesEvent.SubSessionClick -> {
                viewModelScope.launch {
                    val session = _uiState.value.htaSessions.find { it.id == event.parentSessionId }
                        ?: _uiState.value.diabetesSessions.find { it.id == event.parentSessionId }

                    session?.let {
                        val weekNumber = when(it.treatmentType) {
                            TreatmentType.HTA -> it.id
                            TreatmentType.DIABETES -> it.id - 16
                        }
                        val sessionTitle = "Semana $weekNumber"

                        _navigationEvent.send(NavigationEvent.NavigateToMultimediaSelector(
                            event.parentSessionId,
                            sessionTitle,
                            event.subSessionId,
                            event.title,
                            event.treatmentType.name
                        ))
                    }
                }
            }
            SesionesEvent.BackClicked -> {
                 viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateBack)
                }
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val hasHta = _uiState.value.treatmentTypes.contains(TreatmentType.HTA)
                val hasDiabetes = _uiState.value.treatmentTypes.contains(TreatmentType.DIABETES)

                val htaSessions = if (hasHta) repository.getHtaSessions() else emptyList()
                val diabetesSessions = if (hasDiabetes) repository.getDiabetesSessions() else emptyList()

                _uiState.update {
                    it.copy(
                        htaSessions = htaSessions,
                        diabetesSessions = diabetesSessions,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
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
        data class NavigateToMultimediaSelector(
            val parentSessionId: Long,
            val sessionTitle: String,
            val subSessionId: Long,
            val subSessionTitle: String,
            val treatmentType: String
        ) : NavigationEvent()
    }
}
