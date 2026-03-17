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
    detailViewState: TableDetailScreenState,
) {
    // Patch 1: 상세 화면의 실제 렌더링은 기존 TableEditorScreen 기반을 유지하되,
    // ui/table/detail 축에서 ViewModel state를 구독하고 있음을 명시해 다음 패치 주도권 이전 준비를 한다.
    @Suppress("UNUSED_VARIABLE")
    val keepDetailAxisAlive = detailViewState
    TableEditorScreen(
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onReset = onReset,
        onBack = onBack,
    )
}
