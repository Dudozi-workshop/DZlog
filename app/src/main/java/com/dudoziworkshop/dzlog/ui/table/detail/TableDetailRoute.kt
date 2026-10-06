package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

@Composable
fun TableDetailRoute(
    templateState: TableTemplateState,
    templateName: String = "",
    isUnsavedNewTemplate: Boolean = false,
    onTemplateChange: (TableTemplateState) -> Unit,
    onDiscardUnsavedNewTemplate: () -> Unit = {},
    onBack: () -> Unit,
) {
    TableDetailScreen(
        templateState = templateState,
        templateName = templateName,
        isUnsavedNewTemplate = isUnsavedNewTemplate,
        onTemplateChange = onTemplateChange,
        onDiscardUnsavedNewTemplate = onDiscardUnsavedNewTemplate,
        onBack = onBack,
    )
}
