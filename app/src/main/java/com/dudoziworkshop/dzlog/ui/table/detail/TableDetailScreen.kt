package com.dudoziworkshop.dzlog.ui.table.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableEditMode
import com.dudoziworkshop.dzlog.feature.table.state.TableDetailScreenState
import com.dudoziworkshop.dzlog.ui.table.TableEditorScreen
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

internal data class TableDetailReadOnlyUiState(
    val isStructureMode: Boolean,
    val canUndo: Boolean,
    val selectedCount: Int,
    val hasSelectionRange: Boolean,
)

@Composable
fun TableDetailScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onBack: () -> Unit,
    detailViewState: TableDetailScreenState,
    onDetailAction: (TableDetailAction) -> TableTemplateState,
    onSyncTemplateToDetail: (TableTemplateState, Boolean) -> Unit,
) {
    // 읽기 전용 연결 단계: detail state를 실제 읽되 editor 내부 SSOT(local state)는 유지한다.
    val readOnlyState = remember(detailViewState) {
        TableDetailReadOnlyUiState(
            isStructureMode = detailViewState.editMode == TableEditMode.Structure,
            canUndo = detailViewState.canUndo,
            selectedCount = detailViewState.selection.selectedCellIds.size,
            hasSelectionRange = detailViewState.selection.minRow != null,
        )
    }

    // 쓰기 연결은 다음 패치에서 수행한다. 이번 단계는 route 레벨 계약을 유지하기 위해 인자만 보관한다.
    remember(onDetailAction, onSyncTemplateToDetail) { }

    Column {
        TableDetailReadOnlyIndicator(
            readOnlyState = readOnlyState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        TableEditorScreen(
            templateState = templateState,
            onTemplateChange = onTemplateChange,
            onBack = onBack,
        )
    }
}

@Composable
private fun TableDetailReadOnlyIndicator(
    readOnlyState: TableDetailReadOnlyUiState,
    modifier: Modifier = Modifier,
) {
    val modeLabel = if (readOnlyState.isStructureMode) "구조" else "일반"
    val rangeLabel = if (readOnlyState.hasSelectionRange) "범위선택 있음" else "범위선택 없음"
    val undoLabel = if (readOnlyState.canUndo) "Undo 가능" else "Undo 없음"
    Text(
        text = "Detail 읽기 상태 · 모드:$modeLabel · 선택:${readOnlyState.selectedCount} · $rangeLabel · $undoLabel",
        modifier = modifier,
        style = DDZTypography.Caption,
        color = DDZColor.TextMuted,
    )
}
