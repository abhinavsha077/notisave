package com.echo.inbox.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class EchoColorPalette(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val mint: Color = Color(0xFF66E8C3),
    val signalBlue: Color = Color(0xFF6FA8FF),
    val warning: Color = Color(0xFFFFB020),
    val danger: Color = Color(0xFFFF5C5C),
    val mintContainer: Color,
    val onMintContainer: Color,
    val blueContainer: Color,
    val onBlueContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

val EchoLightPalette = EchoColorPalette(
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFF1F5F9),
    primaryText = Color(0xFF0C0F14),
    secondaryText = Color(0xFF657386),
    border = Color(0xFFE5EAF0),
    mint = Color(0xFF00875A),
    signalBlue = Color(0xFF3B82F6),
    warning = Color(0xFFD97706),
    danger = Color(0xFFDC2626),
    mintContainer = Color(0xFFE6F8F2),
    onMintContainer = Color(0xFF005A3C),
    blueContainer = Color(0xFFEFF6FF),
    onBlueContainer = Color(0xFF1D4ED8),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF92400E),
    dangerContainer = Color(0xFFFEE2E2),
    onDangerContainer = Color(0xFF991B1B),
)

val EchoDarkPalette = EchoColorPalette(
    background = Color(0xFF0A1017),
    surface = Color(0xFF111A24),
    elevated = Color(0xFF182331),
    primaryText = Color(0xFFF7F9FC),
    secondaryText = Color(0xFF9AA7B7),
    border = Color(0xFF263243),
    mint = Color(0xFF66E8C3),
    signalBlue = Color(0xFF6FA8FF),
    warning = Color(0xFFFFB020),
    danger = Color(0xFFFF5C5C),
    mintContainer = Color(0xFF0D3328),
    onMintContainer = Color(0xFF66E8C3),
    blueContainer = Color(0xFF142C4E),
    onBlueContainer = Color(0xFF93C5FD),
    warningContainer = Color(0xFF3B2607),
    onWarningContainer = Color(0xFFFDE68A),
    dangerContainer = Color(0xFF3C1414),
    onDangerContainer = Color(0xFFFCA5A5),
)

object EchoSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
}

object EchoRadius {
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
}

val LocalEchoColors = staticCompositionLocalOf { EchoLightPalette }

object EchoTheme {
    val colors: EchoColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalEchoColors.current

    val spacing: EchoSpacing
        get() = EchoSpacing

    val radius: EchoRadius
        get() = EchoRadius
}
