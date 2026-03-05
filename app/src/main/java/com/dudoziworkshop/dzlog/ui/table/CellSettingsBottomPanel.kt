package com.dudoziworkshop.dzlog.ui.table

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.ui.table.template.addToFileNameSlots
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.Date

@Composable
internal fun CellSettingsBottomPanel(
    modifier: Modifier,
    cell: TableCellState,
    templateState: TableTemplateState,
    hasGroup1: Boolean,
    hasGroup2: Boolean,
    onToggleFileNameForCell: (cellId: String, enabled: Boolean) -> Unit,
    onReorderFileNameSlots: (fromIndex: Int, toIndex: Int) -> Unit,
    onPathGroupAction: (PathGroupAction) -> Unit,
    onSetDataType: (TableCellDataType) -> Unit,
    onSetCounterScopeMode: (CounterScopeMode) -> Unit,
    onResetCounterSeed: (() -> Unit)? = null,
    autoNextCounterValue: Int = 1,
    onOpenRotatingTemplateDialog: (() -> Unit)? = null,
    onOpenFormatDialog: (cellId: String, type: TableCellDataType) -> Unit,
    previewNow: Date,
    previewCounterDigits: Int,
    scopeNextCounter: Int,
    dateFormat: String,
    timeFormat: String,
    phraseSets: List<RotatingPhraseSet>
) {
    val context = LocalContext.current
    val isIncluded = templateState.fileNameSlots.contains(cell.cellId)
    var isSlotEditMode by remember { mutableStateOf(false) }
    var selectedFromIndex by remember { mutableStateOf<Int?>(null) }
    var isCounterScopeDialogOpen by remember { mutableStateOf(false) }
    var pendingCounterScopeMode by remember {
        mutableStateOf(cell.counterScopeMode ?: CounterScopeMode.EXCLUDE)
    }
    val panelScrollState = rememberScrollState()

    val density = LocalDensity.current
    val maxPanelHeight = with(density) {
        LocalWindowInfo.current.containerSize.height.toDp() * 0.5f
    }


    androidx.compose.runtime.LaunchedEffect(cell.cellId, cell.counterScopeMode) {
        pendingCounterScopeMode = cell.counterScopeMode ?: CounterScopeMode.EXCLUDE
    }

    val resolver = remember { TableResolver() }
    val resolvedByCellId = remember(
        resolver,
        templateState.cells,
        previewNow,
        previewCounterDigits,
        scopeNextCounter,
        dateFormat,
        timeFormat,
        phraseSets
    ) {
        resolver.plan(
            cells = templateState.cells,
            captureNow = previewNow,
            config = TableResolver.Config(
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            counterSeedOverride = scopeNextCounter,
            phraseSets = phraseSets
        ).resolvedCells.associate { it.id to it.resolvedText }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxPanelHeight)
            .background(DDZColor.Surface, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(panelScrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("파일명", style = DDZTypography.Body, color = DDZColor.TextMuted)
                    Button(
                        modifier = Modifier.height(30.dp),
                        onClick = {
                            val enable = !isIncluded
                            if (enable) {
                                val nextSlots = addToFileNameSlots(templateState.fileNameSlots, cell.cellId)
                                if (nextSlots == templateState.fileNameSlots) {
                                    Toast.makeText(context, "파일명은 최대 3개까지 설정할 수 있습니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    onToggleFileNameForCell(cell.cellId, true)
                                }
                            } else {
                                isSlotEditMode = false
                                selectedFromIndex = null
                                onToggleFileNameForCell(cell.cellId, false)
                            }
                        }
                    ) {
                        Text(if (isIncluded) "ON" else "OFF", style = DDZTypography.Caption)
                    }
                }

                if (isIncluded) {
                    Text(
                        text = if (isSlotEditMode) "슬롯 편집 중: 두 칸을 탭해 순서를 바꾸세요" else "슬롯을 길게 눌러 편집",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextMuted
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DDZColor.Card, RoundedCornerShape(14.dp))
                            .padding(7.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(FILE_NAME_SLOT_COUNT) { index ->
                            val slotCellId = templateState.fileNameSlots.getOrNull(index)
                            val slotText = when {
                                slotCellId == null -> "비어있음"
                                else -> resolvedByCellId[slotCellId].orEmpty().ifBlank { "(값 없음)" }
                            }
                            val isSelected = slotCellId == cell.cellId
                            val isFromSelected = isSlotEditMode && selectedFromIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        color = if (isSelected) DDZColor.Primary else DDZColor.Surface,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .border(
                                        width = if (isFromSelected) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isFromSelected) DDZColor.SageDark else if (isSelected) DDZColor.Primary else DDZColor.Border,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .pointerInput(isSlotEditMode, selectedFromIndex) {
                                        detectTapGestures(
                                            onLongPress = {
                                                isSlotEditMode = !isSlotEditMode
                                                selectedFromIndex = null
                                            },
                                            onTap = {
                                                if (!isSlotEditMode) {
                                                    Toast.makeText(context, "길게 눌러 편집", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val from = selectedFromIndex
                                                    if (from == null) {
                                                        selectedFromIndex = index
                                                    } else {
                                                        if (from != index) {
                                                            onReorderFileNameSlots(from, index)
                                                        }
                                                        selectedFromIndex = null
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slotText,
                                    style = DDZTypography.ButtonText,
                                    color = if (isSelected) Color.White else DDZColor.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text("데이터 형식", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
            DataTypeCardGrid3(
                selected = cell.dataType,
                onSelect = onSetDataType
            )

            if (cell.dataType == TableCellDataType.COUNTER && onResetCounterSeed != null) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onResetCounterSeed
                ) {
                    Text("카운터 초기화 ($autoNextCounterValue)", style = DDZTypography.ButtonText)
                }
            }

            if (cell.dataType == TableCellDataType.DATE || cell.dataType == TableCellDataType.TIME) {
                val formatLabel = if (cell.dataType == TableCellDataType.DATE) "날짜 형식" else "시간 형식"
                val scopeLabel = when (cell.counterScopeMode ?: CounterScopeMode.EXCLUDE) {
                    CounterScopeMode.EXCLUDE -> "스코프: 제외"
                    CounterScopeMode.INCLUDE -> "스코프: 포함"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFormatDialog(cell.cellId, cell.dataType) }
                    ) {
                        Text(formatLabel, style = DDZTypography.ButtonText)
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pendingCounterScopeMode = cell.counterScopeMode ?: CounterScopeMode.EXCLUDE
                            isCounterScopeDialogOpen = true
                        }
                    ) {
                        Text(scopeLabel, style = DDZTypography.ButtonText)
                    }
                }
            }


            if (isCounterScopeDialogOpen && (cell.dataType == TableCellDataType.DATE || cell.dataType == TableCellDataType.TIME)) {
                AlertDialog(
                    onDismissRequest = { isCounterScopeDialogOpen = false },
                    title = { Text("카운터 스코프", style = DDZTypography.Body, color = DDZColor.TextPrimary) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "이 날짜/시간 값이 바뀌면 카운터도 분리됩니다.",
                                style = DDZTypography.Caption,
                                color = DDZColor.TextMuted
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pendingCounterScopeMode = CounterScopeMode.EXCLUDE },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = pendingCounterScopeMode == CounterScopeMode.EXCLUDE,
                                    onClick = { pendingCounterScopeMode = CounterScopeMode.EXCLUDE }
                                )
                                Text("포함 안 함", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pendingCounterScopeMode = CounterScopeMode.INCLUDE },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = pendingCounterScopeMode == CounterScopeMode.INCLUDE,
                                    onClick = { pendingCounterScopeMode = CounterScopeMode.INCLUDE }
                                )
                                Text("포함", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onSetCounterScopeMode(pendingCounterScopeMode)
                                isCounterScopeDialogOpen = false
                            }
                        ) {
                            Text("확인")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isCounterScopeDialogOpen = false }) {
                            Text("취소")
                        }
                    }
                )
            }

            if (cell.dataType == TableCellDataType.ROTATING_TEXT && onOpenRotatingTemplateDialog != null) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenRotatingTemplateDialog
                ) {
                    Text("순환 문구 설정", style = DDZTypography.ButtonText)
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DataTypeCardGrid3(
    selected: TableCellDataType,
    onSelect: (TableCellDataType) -> Unit
) {
    val items = listOf(
        Triple(TableCellDataType.TEXT, Icons.Default.TextFields, "텍스트"),
        Triple(TableCellDataType.NUMBER, Icons.Default.Numbers, "숫자"),
        Triple(TableCellDataType.DATE, Icons.Default.DateRange, "날짜"),
        Triple(TableCellDataType.TIME, Icons.Default.AccessTime, "시간"),
        Triple(TableCellDataType.COUNTER, Icons.Default.ExposurePlus1, "카운터"),
        Triple(TableCellDataType.ROTATING_TEXT, Icons.Default.Autorenew, "순환 문구")
    )

    val rows = items.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (type, icon, koLabel) ->
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
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Icon(
                                imageVector = icon,
                                contentDescription = koLabel,
                                tint = DDZColor.TextPrimary,
                                modifier = Modifier.height(30.dp)
                            )
                            Text(
                                text = koLabel,
                                style = DDZTypography.Caption,
                                color = DDZColor.TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = if (type == TableCellDataType.ROTATING_TEXT) 2 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
