package com.colma.colmaApp.features.MenuMain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colma.colmaApp.features.MenuMain.data.MenuMainRepositoryImpl
import com.colma.colmaApp.servicios.database.dbhelper.DbHelper

@Suppress("UNCHECKED_CAST")
class MenuMainViewModelFactory(
    private val repository: MenuMainRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuMainViewModel::class.java)) {
            return MenuMainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}