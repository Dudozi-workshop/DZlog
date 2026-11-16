package com.dudoziworkshop.dzlog.ui.table.mock

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.policy.QuickCellLinks
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@Composable
internal fun TableEditorWorkPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalDDZColor.current
    val radius = DDZLayout.Radius.Medium
    Surface(
        modifier = modifier.drawWithContent {
            drawContent()
            val r = minOf(radius.toPx(), size.width / 2, size.height)
            val edge = Path().apply {
                moveTo(0f, r)
                quadraticBezierTo(0f, 0f, r, 0f)
                lineTo(size.width - r, 0f)
                quadraticBezierTo(size.width, 0f, size.width, r)
            }
            drawPath(edge, colors.Border, style = Stroke(1.dp.toPx()))
        },
        color = colors.Surface,
        shape = RoundedCornerShape(topStart = radius, topEnd = radius),
        content = content,
    )
}

/** Stable domain identity: row/column changes can renumber temporary UI indexes. */
internal fun editorCellStateKey(cell: TableEditorCellUiModel): String =
    cell.domainCellId ?: "ui:${cell.id}"

/** Only text and numeric editors can accept a continued IME session. */
internal fun editorCanKeepKeyboard(type: TableEditorCellType?): Boolean =
    type == TableEditorCellType.TEXT || type == TableEditorCellType.NUMBER

internal data class EditorCellNeighbours(val previous: String?, val next: String?)

/** Detail editing includes every kind; merged covered cells never become stops. */
internal fun editorCellNeighbours(cells: List<TableEditorCellUiModel>, selectedId: String?): EditorCellNeighbours {
    val ids = cells.filter { !it.isCovered && it.domainCellId != null }
        .sortedWith(compareBy({ it.rowIndex }, { it.colIndex })).mapNotNull { it.domainCellId }.distinct()
    val index = ids.indexOf(selectedId)
    return if (index < 0) EditorCellNeighbours(null, null)
    else EditorCellNeighbours(ids.getOrNull(index - 1), ids.getOrNull(index + 1))
}

@Composable
internal fun TableEditorCellInfoRow(
    cell: TableEditorCellUiModel,
    links: QuickCellLinks,
    neighbours: EditorCellNeighbours,
    enabled: Boolean,
    onMove: (String) -> Unit,
    onTypeChange: (TableEditorCellType) -> Unit,
    onClose: () -> Unit,
    compactInput: Boolean = false,
    onInputDone: () -> Unit = {},
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    val context = LocalContext.current
    var showTypePicker by remember(cell.id) { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f)
                .heightIn(min = DDZLayout.QuickInput.Touch)
                .clickable(enabled = links.count > 0) {
                    Toast.makeText(context, links.description, Toast.LENGTH_SHORT).show()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "${cell.rowIndex + 1}행 ${cell.colIndex + 1}열",
                style = typography.Caption,
                color = colors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (links.fileName) Icon(
                Icons.Filled.Description, links.description,
                Modifier.size(DDZLayout.QuickInput.CellMarker), tint = colors.SelectionEmphasis,
            )
            if (links.path) Icon(
                Icons.Filled.Folder, if (links.fileName) null else links.description,
                Modifier.size(DDZLayout.QuickInput.CellMarker), tint = colors.SelectionEmphasis,
            )
        }

        if (!compactInput) Surface(
            onClick = { showTypePicker = true },
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            color = colors.Surface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(cell.type.choiceIcon(), contentDescription = null, Modifier.size(16.dp))
                Text(cell.type.label, style = typography.Caption, maxLines = 1)
                Icon(Icons.Filled.ArrowDropDown, "셀 종류 변경", Modifier.size(18.dp))
            }
        }

        IconButton(
            onClick = { neighbours.previous?.let(onMove) },
            enabled = enabled && neighbours.previous != null,
            modifier = Modifier.size(DDZLayout.QuickInput.Touch),
        ) {
            Icon(Icons.Filled.ChevronLeft, "이전 셀", Modifier.size(DDZLayout.QuickInput.Icon))
        }
        IconButton(
            onClick = { neighbours.next?.let(onMove) },
            enabled = enabled && neighbours.next != null,
            modifier = Modifier.size(DDZLayout.QuickInput.Touch),
        ) {
            Icon(Icons.Filled.ChevronRight, "다음 셀", Modifier.size(DDZLayout.QuickInput.Icon))
        }
        if (compactInput) {
            TextButton(onClick = onInputDone, enabled = enabled) { Text("완료") }
        } else {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(DDZLayout.QuickInput.Touch),
            ) {
                Icon(Icons.Filled.Close, "셀 편집 닫기", Modifier.size(DDZLayout.QuickInput.Icon))
            }
        }
    }
    if (showTypePicker) {
        TableEditorCellTypeSheet(
            selectedType = cell.type,
            onSelect = {
                onTypeChange(it)
                showTypePicker = false
            },
            onDismiss = { showTypePicker = false },
        )
    }
}
