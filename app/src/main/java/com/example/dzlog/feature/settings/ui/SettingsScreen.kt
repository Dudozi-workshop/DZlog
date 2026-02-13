package com.example.dzlog.feature.settings.ui

import androidx.compose.runtime.Composable
import com.example.dzlog.domain.model.TableTemplateState

@Composable
fun SettingsScreen(
    tableTemplateStateProvider: () -> TableTemplateState,
    onBack: () -> Unit,
    onOpenTableDetail: () -> Unit
) {
    SettingsRootScreen(
        tableTemplateStateProvider = tableTemplateStateProvider,
        onBack = onBack,
        onOpenTableDetail = onOpenTableDetail
    )
}
