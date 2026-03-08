package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.runtime.Composable

/**
 * Deprecated: 기존 호출부 호환을 위한 래퍼.
 * 실질 테마 source는 DDZTheme 단일 진입점으로 통합한다.
 */
@Deprecated(
    message = "Use DDZTheme as the single app theme entry point.",
    replaceWith = ReplaceWith("DDZTheme(content = content)")
)
@Composable
fun DZlogTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    DDZTheme(
        darkTheme = darkTheme,
        content = content
    )
}
