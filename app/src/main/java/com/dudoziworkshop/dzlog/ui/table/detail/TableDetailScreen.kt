package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen

@Composable
fun TableDetailScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    // 기존 상세설정 화면 골격(상단 프리뷰 + 하단패널)을 유지하기 위해
    // TableEditorScreen 기반 조합을 유지한다.
    TableEditorScreen(
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onReset = onReset,
        onBack = onBack,
    )
}
