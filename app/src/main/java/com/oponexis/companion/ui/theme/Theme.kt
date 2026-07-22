package com.oponexis.companion.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = Color(0xFF003A91),
    secondary = BrandMint,
    onSecondary = Color(0xFF00382E),
    background = Cloud,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Slate,
    outline = Color(0xFFD0D5DD),
    error = Color(0xFFD92D20),
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color(0xFF002E73),
    primaryContainer = Color(0xFF124B9C),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = BrandMint,
    onSecondary = Color(0xFF00382E),
    background = Night,
    onBackground = Color(0xFFF3F6FC),
    surface = NightSurface,
    onSurface = Color(0xFFF3F6FC),
    surfaceVariant = Color(0xFF1A263A),
    onSurfaceVariant = Color(0xFFB9C3D4),
    outline = NightOutline,
    error = Color(0xFFFFB4AB),
)

@Composable
fun OponexisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = OponexisTypography,
        content = content,
    )
}
