package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen

@Composable
fun TableDetailScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onBack: () -> Unit,
) {
    TableEditorScreen(
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onBack = onBack,
    )
}
