package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.state.TableDetailScreenState
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen

@Composable
@Suppress("UNUSED_PARAMETER")
fun TableDetailScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onBack: () -> Unit,
    detailViewState: TableDetailScreenState,
    onDetailAction: (TableDetailAction) -> TableTemplateState,
    onSyncTemplateToDetail: (TableTemplateState, Boolean) -> Unit,
) {
    TableEditorScreen(
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onBack = onBack,
    )
}
