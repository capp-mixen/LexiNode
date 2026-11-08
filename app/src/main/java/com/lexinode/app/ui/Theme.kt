package com.lexinode.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5DA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E2FF),
    onPrimaryContainer = Color(0xFF001A40),
    secondary = Color(0xFF565E71),
    tertiary = Color(0xFF715573),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFABC7FF),
    onPrimary = Color(0xFF002F67),
    primaryContainer = Color(0xFF0E448F),
    onPrimaryContainer = Color(0xFFD7E2FF),
    secondary = Color(0xFFBEC6DC),
    tertiary = Color(0xFFDEBCDF),
    background = Color(0xFF1A1B1F),
    surface = Color(0xFF1A1B1F),
)

@Composable
fun LexiNodeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
