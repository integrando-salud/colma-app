package com.colma.colmaApp.utils

data class TopBarConfig(
    val title: String = "",
    val showBackButton: Boolean = false,
    val onBackClick: (() -> Unit)? = null,
    val showMenuButton: Boolean = true,
    val showTopBar: Boolean = true
)
