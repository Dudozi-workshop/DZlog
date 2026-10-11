package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.policy.TableEditorPolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun MockLayoutPanel(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    rows: Int,
    cols: Int,
    mergedSelection: Boolean,
    onAddRow: () -> Unit,
    onAddCol: () -> Unit,
    onMergeSelection: () -> Unit,
    onDeleteRows: () -> Unit,
    onDeleteColumns: () -> Unit,
    onEqualizeColumns: () -> Unit,
    onEqualizeRows: () -> Unit,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .background(DDZColor.Surface).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StructureActionButton(Modifier.weight(1f), Icons.Filled.ViewStream,
                if (rows >= TableEditorPolicy.MAX_ROWS) "최대 ${TableEditorPolicy.MAX_ROWS}행" else "행 추가",
                operation = "+", enabled = rows < TableEditorPolicy.MAX_ROWS, onClick = onAddRow)
            StructureActionButton(Modifier.weight(1f), Icons.Filled.ViewColumn,
                if (cols >= TableEditorPolicy.MAX_COLS) "최대 ${TableEditorPolicy.MAX_COLS}열" else "열 추가",
                operation = "+", enabled = cols < TableEditorPolicy.MAX_COLS, onClick = onAddCol)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StructureActionButton(Modifier.weight(1f), Icons.Filled.ViewStream, "행 삭제",
                operation = "−", enabled = selectedCount > 0 && rows > 1, danger = true, onClick = onDeleteRows)
            StructureActionButton(Modifier.weight(1f), Icons.Filled.ViewColumn, "열 삭제",
                operation = "−", enabled = selectedCount > 0 && cols > 1, danger = true, onClick = onDeleteColumns)
        }
        StructureActionButton(Modifier.fillMaxWidth(), Icons.Filled.GridView,
            if (mergedSelection) "병합 해제" else "병합", enabled = selectedCount > 1 || mergedSelection,
            onClick = onMergeSelection)
        HorizontalDivider(color = DDZColor.Border)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StructureActionButton(Modifier.weight(1f), Icons.Filled.ViewColumn, "열 너비 균등",
                enabled = cols > 1, onClick = onEqualizeColumns)
            StructureActionButton(Modifier.weight(1f), Icons.Filled.ViewStream, "행 높이 균등",
                enabled = rows > 1, onClick = onEqualizeRows)
        }

    }
}

@Composable
private fun StructureActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    operation: String? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        danger -> DDZColor.Destructive.copy(alpha = if (enabled) 1f else 0.45f)
        !enabled -> DDZColor.IconMuted
        else -> DDZColor.TextPrimary
    }
    Row(modifier.heightIn(min = structureActionHeight(LocalDensity.current.fontScale))
        .background(if (enabled) DDZColor.Card.copy(alpha = 0.72f) else DDZColor.Card.copy(alpha = 0.36f), RoundedCornerShape(12.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        if (operation != null) {
            Spacer(Modifier.width(2.dp))
            Text(operation, color = tint, style = DDZTypography.Caption)
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = tint, style = DDZTypography.Caption, maxLines = 1)
    }
}
// Four action rows, four 4dp gaps, one divider and 8dp top/bottom padding.
internal fun structureActionHeight(fontScale: Float) = (44f * fontScale.coerceAtLeast(1f)).dp
internal fun structurePanelHeight(fontScale: Float) = structureActionHeight(fontScale) * 4 + 33.dp
