package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.state.TableDetailScreenState
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen

@Composable
fun TableDetailScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onBack: () -> Unit,
    detailViewState: TableDetailScreenState,
    onDetailAction: (TableDetailAction) -> TableTemplateState,
    onSyncTemplateToDetail: (TableTemplateState, Boolean) -> Unit,
) {
    // detail 축 계약은 유지하되, 이번 단계에서는 화면에 임시 상태 UI를 노출하지 않는다.
    remember(detailViewState, onDetailAction, onSyncTemplateToDetail) { }

    TableEditorScreen(
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onBack = onBack,
    )
}
