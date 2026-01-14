package com.codeco.tabacaleros.features.Menu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.codeco.tabacaleros.features.Menu.domain.MenuRepository

@Suppress("UNCHECKED_CAST")
class MenuViewModelFactory(private val menuRepository: MenuRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            return MenuViewModel(menuRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
