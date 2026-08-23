package com.yaarapp.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = YaarOrange,
    onPrimary = Color.White,
    primaryContainer = YaarOrangeLight,
    onPrimaryContainer = YaarCharcoal,
    secondary = YaarGreen,
    onSecondary = Color.White,
    secondaryContainer = YaarGreenLight,
    onSecondaryContainer = Color.White,
    tertiary = YaarGreenDark,
    background = YaarCream,
    onBackground = YaarCharcoal,
    surface = YaarSurface,
    onSurface = YaarCharcoal,
    surfaceVariant = YaarCream,
    error = YaarError
)

private val DarkColors = darkColorScheme(
    primary = YaarOrangeLight,
    onPrimary = YaarCharcoal,
    primaryContainer = YaarOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = YaarGreenLight,
    onSecondary = YaarCharcoal,
    secondaryContainer = YaarGreenDark,
    onSecondaryContainer = Color.White,
    tertiary = YaarGreenLight,
    background = YaarCharcoal,
    onBackground = Color.White,
    surface = Color(0xFF1F1F1F),
    onSurface = Color.White,
    error = YaarError
)

@Composable
fun YaarAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YaarTypography,
        content = content
    )
}
