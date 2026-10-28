package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF002028),
    primaryContainer = NeonCyanDark,
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = CrystalViolet,
    onSecondary = Color(0xFF22005D),
    secondaryContainer = Color(0xFF381E72),
    onSecondaryContainer = Color(0xFFEDE7F6),
    tertiary = ToxicEmerald,
    onTertiary = Color(0xFF003919),
    error = DangerRed,
    onError = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder
)

@Composable
fun CristalZombieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Cristal Zombie is styled with an immersive dark arcade atmosphere
    MaterialTheme(
        colorScheme = GameDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CristalZombieTheme(content = content)
}
