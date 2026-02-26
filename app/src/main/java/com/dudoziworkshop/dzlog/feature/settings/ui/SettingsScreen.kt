package com.dudoziworkshop.dzlog.feature.settings.ui

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

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
