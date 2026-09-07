package com.example.ui.theme

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
import com.example.data.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = ElegantLavenderPrimary,
    onPrimary = ElegantLavenderOnPrimary,
    primaryContainer = ElegantLavenderPrimaryContainer,
    onPrimaryContainer = ElegantLavenderOnPrimaryContainer,
    secondary = ElegantSecondary,
    onSecondary = ElegantOnSecondary,
    tertiary = ElegantTertiary,
    onTertiary = ElegantOnTertiary,
    background = ElegantDarkBackground,
    onBackground = ElegantDarkTextPrimary,
    surface = ElegantDarkSurface,
    onSurface = ElegantDarkTextPrimary,
    surfaceVariant = ElegantDarkSurfaceVariant,
    onSurfaceVariant = ElegantDarkTextSecondary,
    outline = ElegantDarkOutline,
    outlineVariant = ElegantDarkSurfaceHighlight,
    error = ElegantDarkError,
    onError = ElegantDarkOnError
)

private val LightColorScheme = lightColorScheme(
    primary = ElegantLightPrimary,
    onPrimary = ElegantLightOnPrimary,
    primaryContainer = ElegantLightPrimaryContainer,
    onPrimaryContainer = ElegantLightOnPrimaryContainer,
    background = ElegantLightBackground,
    onBackground = Color(0xFF1D1B1E),
    surface = ElegantLightSurface,
    onSurface = Color(0xFF1D1B1E),
    surfaceVariant = ElegantLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF49454E),
    outline = ElegantLightOutline
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
