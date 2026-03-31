package com.prashant.droidkit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DroidKitColors {
    val StorageBlue = Color(0xFF185FA5)
    val LinkGreen = Color(0xFF3B6D11)
    val NotifAmber = Color(0xFFBA7517)
    val FireButton = Color(0xFF1D9E75)
    val DarkSurface = Color(0xFF1A1A1A)
    val CardSurface = Color(0xFF2A2A2A)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B0B0)
    val ErrorRed = Color(0xFFCF6679)
    val WarningYellow = Color(0xFFFFD54F)
}

private val DroidKitColorScheme = darkColorScheme(
    primary = DroidKitColors.StorageBlue,
    secondary = DroidKitColors.LinkGreen,
    tertiary = DroidKitColors.NotifAmber,
    surface = DroidKitColors.DarkSurface,
    surfaceVariant = DroidKitColors.CardSurface,
    onSurface = DroidKitColors.TextPrimary,
    onSurfaceVariant = DroidKitColors.TextSecondary,
    error = DroidKitColors.ErrorRed,
    background = Color(0xFF121212),
    onBackground = DroidKitColors.TextPrimary
)

@Composable
fun DroidKitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DroidKitColorScheme,
        content = content
    )
}
