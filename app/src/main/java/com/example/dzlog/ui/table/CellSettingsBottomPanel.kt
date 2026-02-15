package com.example.dzlog.ui.table

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.R
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

@Composable
internal fun CellSettingsBottomPanel(
    modifier: Modifier,
    cell: TableCellState,
    hasGroup1: Boolean,
    hasGroup2: Boolean,
    onSetFileNameInclude: (Boolean) -> Unit,
    onPathGroupAction: (PathGroupAction) -> Unit,
    onSetDataType: (TableCellDataType) -> Unit,
    onResetCounterSeed: (() -> Unit)? = null,
    autoNextCounterValue: Int = 1
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ✅ 상단 핸들(중앙만) - 공간 최소화
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

        // ✅ 파일명 그룹 (한 줄 병기)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("파일명 포함", style = DDZTypography.Body, color = DDZColor.TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (cell.fileNameInclude) "ON" else "OFF", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                    Switch(
                        checked = cell.fileNameInclude,
                        onCheckedChange = onSetFileNameInclude
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("저장경로", style = DDZTypography.Body, color = DDZColor.TextMuted)
                // G2 선택 가능 조건
                // - G1이 반드시 존재해야 함
                // - 현재 셀이 G1이면: "G2가 이미 존재하는 경우에만" G1<->G2 스왑을 위해 허용
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

        if (cell.dataType == TableCellDataType.COUNTER && onResetCounterSeed != null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onResetCounterSeed
            ) {
                Text("카운터 초기화 ($autoNextCounterValue)", style = DDZTypography.ButtonText)
            }
        }

        // ✅ Data Format: 카드형 3열
        Text("데이터 형식", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
        DataTypeCardGrid3(
            selected = cell.dataType,
            onSelect = onSetDataType
        )
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
        TableCellDataType.DATE to stringResource(R.string.label_date),
        TableCellDataType.TIME to "Time",
        TableCellDataType.COUNTER to stringResource(R.string.label_counter)
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
                                color = if (isSelected) DDZColor.Success.copy(alpha = 0.2f) else DDZColor.Surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) DDZColor.Success else DDZColor.Border,
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
                // 3열 맞추기: row가 3개 미만이면 빈 칸 채움
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
