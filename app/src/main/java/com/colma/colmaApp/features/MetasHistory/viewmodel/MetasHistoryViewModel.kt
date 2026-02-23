package com.colma.colmaApp.features.MetasHistory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.MetasHistory.domain.MetasHistoryRepository
import com.colma.colmaApp.servicios.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MetasHistoryViewModel(private val repository: MetasHistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MetasHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMetas()
    }

    fun onEvent(event: MetasHistoryEvent) {
        // Handle events here if any are added
    }

    private fun loadMetas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = SessionManager.getUserId() ?: 1
            val result = repository.getAllMetas(userId)
            result.onSuccess { metas ->
                _uiState.update { it.copy(isLoading = false, metas = metas) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
