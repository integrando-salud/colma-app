package com.colma.colmaApp.features.mood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.mood.domain.MoodRepository

@Suppress("UNCHECKED_CAST")
class MoodQuizViewModelFactory(private val repository: MoodRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoodQuizViewModel::class.java)) {
            return MoodQuizViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
