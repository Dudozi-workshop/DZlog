package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun TableEditorTemplateSelector(name: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "템플릿 교체", onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("템플릿", style = DDZTypography.Caption, color = DDZColor.TextMuted)
        Text(name, modifier = Modifier.weight(1f), style = DDZTypography.Body,
            fontWeight = FontWeight.SemiBold, color = DDZColor.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (enabled) Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
            modifier = Modifier.size(18.dp), tint = DDZColor.Primary)
    }
}

@Composable
internal fun TemplateMiniPreview(template: TableTemplateState) {
    val rows = TableLayoutCalculator.resolveWeights(template.rowWeights, template.rows)
    val cols = TableLayoutCalculator.resolveWeights(template.colWeights, template.cols)
    val roots = TableStructureRangeActions.rootCells(template.cells)
    Canvas(Modifier.size(width = 48.dp, height = 36.dp)) {
        if (rows.isEmpty() || cols.isEmpty()) return@Canvas
        val rowTotal = rows.sum().coerceAtLeast(0.0001f)
        val colTotal = cols.sum().coerceAtLeast(0.0001f)
        roots.forEach { cell ->
            val r = cell.rowIndex.coerceIn(rows.indices)
            val c = cell.colIndex.coerceIn(cols.indices)
            val bottom = (r + cell.rowSpan.coerceAtLeast(1)).coerceAtMost(rows.size)
            val right = (c + cell.colSpan.coerceAtLeast(1)).coerceAtMost(cols.size)
            val origin = Offset(cols.take(c).sum() / colTotal * size.width,
                rows.take(r).sum() / rowTotal * size.height)
            val cellSize = Size(cols.subList(c, right).sum() / colTotal * size.width,
                rows.subList(r, bottom).sum() / rowTotal * size.height)
            drawRect(DDZColor.SelectedSoft, origin, cellSize)
            drawRect(DDZColor.SageBorder, origin, cellSize, style = Stroke(1.dp.toPx()))
        }
    }
}
