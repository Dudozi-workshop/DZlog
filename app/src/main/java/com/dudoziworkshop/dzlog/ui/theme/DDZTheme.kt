package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDDZColor = staticCompositionLocalOf { DDZColor }
val LocalDDZTypography = staticCompositionLocalOf { DDZTypography }
val LocalDDZSpacing = staticCompositionLocalOf { DDZSpacing }

@Composable
fun DDZTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDDZColor provides DDZColor,
        LocalDDZTypography provides DDZTypography,
        LocalDDZSpacing provides DDZSpacing,
        content = content
    )
}
