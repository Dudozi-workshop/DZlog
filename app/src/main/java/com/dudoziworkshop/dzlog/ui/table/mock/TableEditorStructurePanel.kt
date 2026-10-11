package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
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
    onDeleteSelection: () -> Unit,
    onEqualizeColumns: () -> Unit,
    onEqualizeRows: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(DDZColor.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("구조", style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StructureActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ViewStream,
                label = "행 추가",
                onClick = onAddRow,
            )
            StructureActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ViewColumn,
                label = "열 추가",
                onClick = onAddCol,
            )
            StructureActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.GridView,
                label = if (mergedSelection) "병합 해제" else "병합",
                enabled = selectedCount > 1 || mergedSelection,
                onClick = onMergeSelection,
            )
            StructureActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Delete,
                label = "삭제",
                enabled = selectedCount > 0,
                danger = true,
                onClick = onDeleteSelection,
            )
        }

        Text(
            if (selectedCount == 0) "${rows}행 × ${cols}열 · 셀을 선택하면 병합/삭제가 활성화됩니다."
            else "${selectedCount}개 셀 선택됨 · ${rows}행 × ${cols}열",
            color = DDZColor.TextMuted,
            style = DDZTypography.Caption,
        )

        HorizontalDivider(color = DDZColor.Border)
        Text("빠른 정돈", style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DDZButton(
                text = "열 너비 균등",
                onClick = onEqualizeColumns,
                enabled = cols > 1,
                modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary,
                leadingIcon = Icons.Filled.ViewColumn,
                minHeight = 36.dp,
                textStyleOverride = DDZTypography.Caption,
            )
            DDZButton(
                text = "행 높이 균등",
                onClick = onEqualizeRows,
                enabled = rows > 1,
                modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary,
                leadingIcon = Icons.Filled.ViewStream,
                minHeight = 36.dp,
                textStyleOverride = DDZTypography.Caption,
            )
        }
        DDZButton(
            text = "선택 해제",
            onClick = onClearSelection,
            enabled = selectedCount > 0,
            style = DDZButtonStyle.Secondary,
            minHeight = 36.dp,
            textStyleOverride = DDZTypography.Caption,
        )
        Text(
            "표 바깥 ↔ · ↕ 를 드래그해 조절하세요.",
            style = DDZTypography.Caption,
            color = DDZColor.TextMuted,
        )
    }
}

@Composable
private fun StructureActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> DDZColor.IconMuted
        danger -> DDZColor.Destructive
        else -> DDZColor.TextPrimary
    }
    Column(
        modifier = modifier
            .background(
                color = if (enabled) DDZColor.Card.copy(alpha = 0.72f) else DDZColor.Card.copy(alpha = 0.36f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint)
        Text(label, color = tint, style = DDZTypography.Caption, maxLines = 1)
    }
}

