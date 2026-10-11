package com.echo.inbox.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.echo.inbox.core.model.ThemeMode

private val LightMaterialColors = lightColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Color(0xFF1D4ED8),
    secondary = Color(0xFF00875A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6F8F2),
    onSecondaryContainer = Color(0xFF005A3C),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF0C0F14),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0C0F14),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF657386),
    outline = Color(0xFFE5EAF0),
    outlineVariant = Color(0xFFD8E0EA),
)

private val DarkMaterialColors = darkColorScheme(
    primary = Color(0xFF6FA8FF),
    onPrimary = Color(0xFF0A1E3C),
    primaryContainer = Color(0xFF142C4E),
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = Color(0xFF66E8C3),
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF0D3328),
    onSecondaryContainer = Color(0xFF66E8C3),
    tertiary = Color(0xFFFFB020),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF3B2607),
    onTertiaryContainer = Color(0xFFFDE68A),
    error = Color(0xFFFF5C5C),
    onError = Color(0xFF4A0000),
    errorContainer = Color(0xFF3C1414),
    onErrorContainer = Color(0xFFFCA5A5),
    background = Color(0xFF0A1017),
    onBackground = Color(0xFFF7F9FC),
    surface = Color(0xFF111A24),
    onSurface = Color(0xFFF7F9FC),
    surfaceVariant = Color(0xFF182331),
    onSurfaceVariant = Color(0xFF9AA7B7),
    outline = Color(0xFF263243),
    outlineVariant = Color(0xFF334155),
)

@Composable
fun EchoInboxTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val echoPalette = if (darkTheme) EchoDarkPalette else EchoLightPalette
    val materialColors = if (darkTheme) DarkMaterialColors else LightMaterialColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalEchoColors provides echoPalette) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = EchoTypography,
            content = content,
        )
    }
}
