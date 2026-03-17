package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.state.TableDetailScreenState
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen

@Composable
fun TableDetailScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") detailViewState: TableDetailScreenState,
    @Suppress("UNUSED_PARAMETER") onDetailAction: (TableDetailAction) -> TableTemplateState,
    @Suppress("UNUSED_PARAMETER") onSyncTemplateToDetail: (TableTemplateState, Boolean) -> Unit,
) {
    // 안정화 패치: 상세 축(viewModel/state)은 유지하되, editor 내부 state 주도권 침투는 한 단계 뒤로 미룬다.
    TableEditorScreen(
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onReset = onReset,
        onBack = onBack,
    )
}
