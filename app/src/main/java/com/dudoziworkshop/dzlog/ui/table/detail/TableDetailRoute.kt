package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

@Composable
fun TableDetailRoute(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onBack: () -> Unit,
) {
    TableDetailScreen(
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onBack = onBack,
    )
}
