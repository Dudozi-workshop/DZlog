package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalDDZColor = staticCompositionLocalOf { DDZColor }
val LocalDDZTypography = staticCompositionLocalOf { DDZTypography }
val LocalDDZSpacing = staticCompositionLocalOf { DDZSpacing }

// DZlog UI System v1 is light-only.
// Camera/viewer overlays may intentionally render dark surfaces at screen level.
private val DDZLightColorScheme = lightColorScheme(
    primary = DDZColor.Primary,
    onPrimary = DDZColor.OnPrimary,
    secondary = DDZColor.Selected,
    onSecondary = DDZColor.OnPrimary,
    tertiary = DDZColor.SurfaceSoft,
    onTertiary = DDZColor.TextPrimary,
    background = DDZColor.Background,
    onBackground = DDZColor.TextPrimary,
    surface = DDZColor.Surface,
    onSurface = DDZColor.TextPrimary,
    surfaceVariant = DDZColor.SurfaceSoft,
    onSurfaceVariant = DDZColor.TextSecondary,
    outline = DDZColor.Border,
    outlineVariant = DDZColor.BorderStrong,
    error = DDZColor.Destructive,
    onError = DDZColor.OnPrimary,
)

private val DDZMaterialTypography = Typography(
    headlineSmall = DDZTypography.HomeMainTitle,
    titleLarge = DDZTypography.ScreenTitle,
    titleMedium = DDZTypography.SectionTitle,
    titleSmall = DDZTypography.SettingLabel,
    bodyLarge = DDZTypography.Body,
    bodyMedium = DDZTypography.Body,
    bodySmall = DDZTypography.Secondary,
    labelLarge = DDZTypography.ButtonText,
    labelMedium = DDZTypography.SettingLabel,
    labelSmall = DDZTypography.Caption,
)

private val DDZShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun DDZTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DDZLightColorScheme,
        typography = DDZMaterialTypography,
        shapes = DDZShapes,
    ) {
        CompositionLocalProvider(
            LocalDDZColor provides DDZColor,
            LocalDDZTypography provides DDZTypography,
            LocalDDZSpacing provides DDZSpacing,
            content = content,
        )
    }
}
