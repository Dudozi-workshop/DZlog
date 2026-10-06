package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen

@Composable
fun TableDetailScreen(
    templateState: TableTemplateState,
    templateName: String = "",
    isUnsavedNewTemplate: Boolean = false,
    onTemplateChange: (TableTemplateState) -> Unit,
    onDiscardUnsavedNewTemplate: () -> Unit = {},
    onBack: () -> Unit,
) {
    TableEditorScreen(
        templateState = templateState,
        templateName = templateName,
        isUnsavedNewTemplate = isUnsavedNewTemplate,
        onTemplateChange = onTemplateChange,
        onDiscardUnsavedNewTemplate = onDiscardUnsavedNewTemplate,
        onBack = onBack,
    )
}
