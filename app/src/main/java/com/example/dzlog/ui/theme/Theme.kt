package com.example.dzlog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LatteBrown,
    onPrimary = Color.White,

    secondary = SageGreen,
    onSecondary = Color.White,

    tertiary = SandBrown,
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
    primary = SandBrown,
    onPrimary = Color.Black,

    secondary = SageGreenDark,
    onSecondary = Color.Black,

    tertiary = LatteBrown,
    onTertiary = Color.Black,

    background = Color(0xFF15110D),
    onBackground = Color(0xFFEFE4D6),

    surface = Color(0xFF1C1611),
    onSurface = Color(0xFFEFE4D6),

    surfaceVariant = Color(0xFF2A2119),
    onSurfaceVariant = Color(0xFFD8C8B5),

    outline = Color(0xFF8A7A69)
)

@Composable
fun DZlogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}