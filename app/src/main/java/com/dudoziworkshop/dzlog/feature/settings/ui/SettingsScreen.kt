package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit = {},
) {
    SettingsRootScreen(
        onBack = onBack,
        onOpenCredits = onOpenCredits,
    )
}
