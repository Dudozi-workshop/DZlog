package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
internal fun TableEditorCellTypeSheet(
    selectedType: TableEditorCellType,
    onSelect: (TableEditorCellType) -> Unit,
    onDismiss: () -> Unit,
) {
    DDZBottomSheet(title = "셀에 무엇을 표시할까요?", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TableEditorCellType.entries.chunked(2).forEach { types ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    types.forEach { type ->
                        val isSelected = type == selectedType
                        DDZButton(
                            text = type.label + if (isSelected) " ✓" else "",
                            leadingIcon = type.choiceIcon(),
                            modifier = Modifier.weight(1f).semantics { selected = isSelected },
                            minHeight = 56.dp,
                            style = DDZButtonStyle.Secondary,
                            containerColorOverride = if (isSelected) DDZColor.SelectedSoft else DDZColor.Surface,
                            onClick = { onSelect(type) },
                        )
                    }
                }
            }
        }
    }
}

private fun TableEditorCellType.choiceIcon(): ImageVector = when (this) {
    TableEditorCellType.TEXT -> Icons.Filled.TextFields
    TableEditorCellType.NUMBER -> Icons.Filled.Numbers
    TableEditorCellType.COUNTER -> Icons.Filled.Tag
    TableEditorCellType.DATE -> Icons.Filled.DateRange
    TableEditorCellType.TIME -> Icons.Filled.AccessTime
    TableEditorCellType.ROTATING_TEXT -> Icons.Filled.Autorenew
}
