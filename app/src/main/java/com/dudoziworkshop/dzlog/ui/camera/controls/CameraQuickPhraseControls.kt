package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy
import com.dudoziworkshop.dzlog.ui.common.DDZCompactPhraseProgressSheet
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Camera and table detail open the same compact phrase controller. */
@Composable
internal fun CameraQuickPhraseProgressCard(
    cell: TableCellState,
    set: RotatingPhraseSet,
    enabled: Boolean,
    onProgressChange: (Int, Int) -> Unit,
    onPeriodChange: (Boolean, Int) -> Unit,
) {
    var open by remember(cell.cellId, set.id) { mutableStateOf(false) }
    val progress = CellPhraseProgressPolicy.current(cell, set)
    val every = CellPhraseProgressPolicy.everyFor(cell, set, progress.index)
    Surface(
        onClick = { open = true },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DDZColor.Border),
        color = DDZColor.Surface,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("문구 진행 상태", style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
            Text(
                "${set.items[progress.index]} · ${progress.completedShots}/${every}장",
                modifier = Modifier.weight(1f),
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
                tint = DDZColor.TextSecondary)
        }
    }
    if (open) {
        DDZCompactPhraseProgressSheet(
            cellKey = cell.cellId,
            set = set,
            phraseIndex = progress.index,
            completedShots = progress.completedShots,
            legacyEveryOverride = cell.everyOverride,
            onProgressChange = onProgressChange,
            onPeriodChange = onPeriodChange,
            onDismiss = { open = false },
        )
    }
}
