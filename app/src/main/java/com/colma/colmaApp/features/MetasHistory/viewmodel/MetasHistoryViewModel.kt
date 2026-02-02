package com.colma.colmaApp.features.MetasHistory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colma.colmaApp.features.MetasHistory.domain.MetasHistoryRepository
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
            // Assuming user id is 1 for now, as in the original code.
            val result = repository.getAllMetas(1)
            result.onSuccess { metas ->
                _uiState.update { it.copy(isLoading = false, metas = metas) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
