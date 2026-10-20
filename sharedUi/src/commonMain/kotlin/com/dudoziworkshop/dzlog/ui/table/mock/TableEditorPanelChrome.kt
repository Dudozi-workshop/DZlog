package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.policy.QuickCellLinks
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@Composable
fun TableEditorWorkPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
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

data class EditorCellNeighbours(val previous: String?, val next: String?)

/** Detail editing includes every kind; merged covered cells never become stops. */
fun editorCellNeighbours(cells: List<TableEditorCellUiModel>, selectedId: String?): EditorCellNeighbours {
    val ids = cells.filter { !it.isCovered && it.domainCellId != null }
        .sortedWith(compareBy({ it.rowIndex }, { it.colIndex })).mapNotNull { it.domainCellId }.distinct()
    val index = ids.indexOf(selectedId)
    return if (index < 0) EditorCellNeighbours(null, null)
    else EditorCellNeighbours(ids.getOrNull(index - 1), ids.getOrNull(index + 1))
}

@Composable
fun TableEditorCellInfoRow(
    cell: TableEditorCellUiModel,
    links: QuickCellLinks,
    neighbours: EditorCellNeighbours,
    enabled: Boolean,
    onMove: (String) -> Unit,
    onInfo: (String) -> Unit,
) {
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${cell.rowIndex + 1}행 ${cell.colIndex + 1}열", Modifier.weight(1f),
            style = typography.Caption, color = colors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (links.count > 0) IconButton(
            onClick = { onInfo(links.description) },
            modifier = Modifier.size(DDZLayout.QuickInput.Touch),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                if (links.fileName) Icon(Icons.Filled.Description, links.description,
                    Modifier.size(DDZLayout.QuickInput.CellMarker), tint = colors.SelectionEmphasis)
                if (links.path) Icon(Icons.Filled.Folder, if (links.fileName) null else links.description,
                    Modifier.size(DDZLayout.QuickInput.CellMarker), tint = colors.SelectionEmphasis)
            }
        }
        IconButton(onClick = { neighbours.previous?.let(onMove) }, enabled = enabled && neighbours.previous != null,
            modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
            Icon(Icons.Filled.ChevronLeft, "이전 셀", Modifier.size(DDZLayout.QuickInput.Icon))
        }
        IconButton(onClick = { neighbours.next?.let(onMove) }, enabled = enabled && neighbours.next != null,
            modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
            Icon(Icons.Filled.ChevronRight, "다음 셀", Modifier.size(DDZLayout.QuickInput.Icon))
        }
    }
}
