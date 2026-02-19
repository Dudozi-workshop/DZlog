package com.example.dzlog.ui.table

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.ui.table.template.addToFileNameSlots
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

@Composable
internal fun CellSettingsBottomPanel(
    modifier: Modifier,
    cell: TableCellState,
    templateState: TableTemplateState,
    hasGroup1: Boolean,
    hasGroup2: Boolean,
    onToggleFileNameForCell: (cellId: String, enabled: Boolean) -> Unit,
    onPathGroupAction: (PathGroupAction) -> Unit,
    onSetDataType: (TableCellDataType) -> Unit,
    onResetCounterSeed: (() -> Unit)? = null,
    autoNextCounterValue: Int = 1,
    onOpenRotatingTemplateDialog: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isIncluded = templateState.fileNameSlots.contains(cell.cellId)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .background(DDZColor.Border, RoundedCornerShape(4.dp))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("파일명 포함", style = DDZTypography.Body, color = DDZColor.TextMuted)
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!isIncluded) {
                            val nextSlots = addToFileNameSlots(templateState.fileNameSlots, cell.cellId)
                            if (nextSlots == templateState.fileNameSlots) {
                                Toast.makeText(context, "파일명은 최대 3개", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }
                        onToggleFileNameForCell(cell.cellId, !isIncluded)
                    }
                ) {
                    Text(if (isIncluded) "ON" else "OFF", style = DDZTypography.ButtonText)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("저장경로", style = DDZTypography.Body, color = DDZColor.TextMuted)
                val canSelectG2 = hasGroup1 && (
                    cell.groupLevel != GroupLevel.G1 || hasGroup2
                )
                val isNone = cell.groupLevel == GroupLevel.NONE
                val isG1 = cell.groupLevel == GroupLevel.G1
                val isG2 = cell.groupLevel == GroupLevel.G2

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DDZColor.Card, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onPathGroupAction(PathGroupAction.NONE) }
                    ) { Text(if (isNone) "없음 ✓" else "없음", style = DDZTypography.ButtonText) }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onPathGroupAction(PathGroupAction.G1) }
                    ) { Text(if (isG1) "G1 ✓" else "G1", style = DDZTypography.ButtonText) }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onPathGroupAction(PathGroupAction.G2) },
                        enabled = canSelectG2
                    ) { Text(if (isG2) "G2 ✓" else "G2", style = DDZTypography.ButtonText) }
                }

                if (!canSelectG2) {
                    Text("※ G2는 G1 설정 후 사용 가능 (현재 G1 셀에는 G2 설정 불가)", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("파일명 슬롯", style = DDZTypography.Body, color = DDZColor.TextMuted)
            repeat(FILE_NAME_SLOT_COUNT) { index ->
                val slotCellId = templateState.fileNameSlots.getOrNull(index)
                val slotCell = templateState.cells.firstOrNull { it.cellId == slotCellId }
                val slotText = when {
                    slotCell == null -> "비어있음"
                    slotCell.label.isNotBlank() -> slotCell.label
                    slotCell.rawText.isNotBlank() -> slotCell.rawText
                    else -> "(값 없음)"
                }
                val isSelectedSlot = slotCellId == cell.cellId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(DDZColor.Card, RoundedCornerShape(14.dp))
                        .border(
                            width = if (isSelectedSlot) 1.5.dp else 1.dp,
                            color = if (isSelectedSlot) DDZColor.Primary else DDZColor.Border,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextMuted
                    )
                    Text(
                        text = slotText,
                        style = DDZTypography.Body,
                        color = if (slotCell == null) DDZColor.TextMuted else DDZColor.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (cell.dataType == TableCellDataType.COUNTER && onResetCounterSeed != null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onResetCounterSeed
            ) {
                Text("카운터 초기화 ($autoNextCounterValue)", style = DDZTypography.ButtonText)
            }
        }


        if (cell.dataType == TableCellDataType.ROTATING_TEXT && onOpenRotatingTemplateDialog != null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenRotatingTemplateDialog
            ) {
                Text("로테이팅 문구 템플릿 설정", style = DDZTypography.ButtonText)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("데이터 형식", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
        DataTypeCardGrid3(
            selected = cell.dataType,
            onSelect = onSetDataType
        )

        Spacer(Modifier.height(2.dp))
    }
}


@Composable
private fun DataTypeCardGrid3(
    selected: TableCellDataType,
    onSelect: (TableCellDataType) -> Unit
) {
    val items = listOf(
        TableCellDataType.TEXT to "Text",
        TableCellDataType.NUMBER to "Number",
        TableCellDataType.DATE to "Date",
        TableCellDataType.TIME to "Time",
        TableCellDataType.COUNTER to "Counter",
        TableCellDataType.ROTATING_TEXT to "순환 문구"
    )

    val rows = items.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (type, label) ->
                    val isSelected = selected == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp)
                            .background(
                                color = if (isSelected) DDZColor.SageLight.copy(alpha = 0.45f) else DDZColor.Surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) DDZColor.SageDark else DDZColor.Border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(type) }
                            .padding(10.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(label, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                            Text(type.name, style = DDZTypography.Caption, color = DDZColor.TextMuted)
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
