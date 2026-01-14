package com.codeco.tabacaleros.features.utils

import android.app.Activity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.codeco.tabacaleros.R

fun Activity.configureSystemBars() {
    window.statusBarColor = ContextCompat.getColor(this, R.color.principal)
    window.navigationBarColor = ContextCompat.getColor(this, R.color.background)

    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = true
    }
}
