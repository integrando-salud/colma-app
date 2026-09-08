package com.colma.colmaApp.utils

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

data class TopBarConfig(
    val title: String = "",
    val showBackButton: Boolean = false,
    val onBackClick: (() -> Unit)? = null,
    val showMenuButton: Boolean = true,
    val showTopBar: Boolean = true,
    val actions: (@Composable RowScope.() -> Unit)? = null
)
