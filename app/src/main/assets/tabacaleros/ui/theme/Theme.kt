package com.codeco.tabacaleros.ui.theme

import android.app.Activity
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

// Ajusta estos colores a tus recursos si quieres
private val WhiteLightColorScheme = lightColorScheme(
    primary = Color(0xFF67310A),        // color de acento (botones). Cámbialo si quieres.
    onPrimary = Color.White,
    secondary = Color(0xFF6A994E),
    tertiary = Color(0xFFB38867),

    background = Color.White,           // <- todo el fondo será blanco
    surface = Color.White,              // <- las tarjetas/diálogos serán blancos
    onBackground = Color(0xFF111111),   // texto sobre fondo blanco
    onSurface = Color(0xFF111111)       // texto sobre surface blanco
)

@Composable
fun TabacalerosTheme(
    // Forzamos tema claro y deshabilitamos dynamic color por defecto
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // if you still want dynamic on Android 12+, keep this branch
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> WhiteLightColorScheme
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
