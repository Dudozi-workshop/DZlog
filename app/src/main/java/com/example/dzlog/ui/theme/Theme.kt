package com.example.dzlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = Color.White,

    secondary = SageGreen,
    onSecondary = Color.White,

    tertiary = SageGreenLight,
    onTertiary = Color.White,

    background = Beige50,
    onBackground = BrownGray900,

    surface = Beige100,
    onSurface = BrownGray900,

    surfaceVariant = Beige200,
    onSurfaceVariant = BrownGray700,

    outline = BrownGray500
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen,
    onPrimary = Color.Black,

    secondary = SageGreen,
    onSecondary = Color.Black,

    tertiary = SageGreenLight,
    onTertiary = Color.Black,

    background = Color(0xFF15110D),
    onBackground = Color(0xFFEFE4D6),

    surface = Color(0xFF1C1611),
    onSurface = Color(0xFFEFE4D6),

    surfaceVariant = Color(0xFF2A2119),
    onSurfaceVariant = Color(0xFFD8C8B5),

    outline = Color(0xFF8A7A69)
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun DZlogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}