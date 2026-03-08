package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

val LocalDDZColor = staticCompositionLocalOf { DDZColor }
val LocalDDZTypography = staticCompositionLocalOf { DDZTypography }
val LocalDDZSpacing = staticCompositionLocalOf { DDZSpacing }

// DDZ 고정 팔레트 기반 Material3 컬러 스킴 (dynamic color 미사용)
private val DDZLightColorScheme = lightColorScheme(
    primary = DDZColor.Primary,
    onPrimary = Color.White,
    secondary = DDZColor.SagePrimary,
    onSecondary = Color.White,
    tertiary = DDZColor.Sage,
    onTertiary = DDZColor.TextStrong,
    background = DDZColor.Background,
    onBackground = DDZColor.TextPrimary,
    surface = DDZColor.Surface,
    onSurface = DDZColor.TextPrimary,
    surfaceVariant = DDZColor.Card,
    onSurfaceVariant = DDZColor.TextMuted,
    outline = DDZColor.Border,
    outlineVariant = DDZColor.SageBorder,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DDZDarkColorScheme = darkColorScheme(
    primary = DDZColor.SageLight,
    onPrimary = DDZColor.PrimaryDark,
    secondary = DDZColor.Sage,
    onSecondary = DDZColor.PrimaryDark,
    tertiary = DDZColor.SagePrimary,
    onTertiary = Color.White,
    background = Color(0xFF15110D),
    onBackground = Color(0xFFEFE4D6),
    surface = Color(0xFF1C1611),
    onSurface = Color(0xFFEFE4D6),
    surfaceVariant = Color(0xFF2A2119),
    onSurfaceVariant = Color(0xFFD8C8B5),
    outline = Color(0xFF8A7A69),
    outlineVariant = Color(0xFF5E5348),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

// Material3 typography도 DDZ 타이포 톤으로 통일
private val DDZMaterialTypography = Typography(
    headlineSmall = DDZTypography.HomeMainTitle,
    titleLarge = DDZTypography.ScreenTitle,
    titleMedium = DDZTypography.CardTitle,
    titleSmall = DDZTypography.SectionTitle,
    bodyLarge = DDZTypography.Body,
    bodyMedium = DDZTypography.Body,
    bodySmall = DDZTypography.Caption,
    labelLarge = DDZTypography.ButtonText,
    labelMedium = DDZTypography.HomeSectionLabel,
    labelSmall = DDZTypography.Caption
)

// DDZ 컴포넌트 둥근 정도와 Material 기본 shape를 맞추기 위한 전역 기본값
private val DDZShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun DDZTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 정책: dynamic color는 어떤 OS/기기에서도 비활성화 고정
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // dynamicColor 파라미터는 호환용으로 유지하되, 실제 적용은 하지 않는다.
    val colorScheme = if (darkTheme) DDZDarkColorScheme else DDZLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DDZMaterialTypography,
        shapes = DDZShapes
    ) {
        CompositionLocalProvider(
            LocalDDZColor provides DDZColor,
            LocalDDZTypography provides DDZTypography,
            LocalDDZSpacing provides DDZSpacing,
            content = content
        )
    }
}
