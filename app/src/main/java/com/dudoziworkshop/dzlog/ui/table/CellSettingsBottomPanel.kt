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
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.Date

@Composable
internal fun CellSettingsBottomPanel(
    modifier: Modifier,
    cell: TableCellState,
    templateState: TableTemplateState,
    baseResolvedByCellId: Map<String, String>,
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
    phraseProgressCursor: Int,
    dateFormat: String,
    timeFormat: String,
    phraseSets: List<RotatingPhraseSet>,
    showFileNameSection: Boolean = true,
    showPathGroupSection: Boolean = true,
    compactForBottomPanel: Boolean = false
) {
    val context = LocalContext.current
    val derivedFileNameSlots = deriveFileNameCellSlotsFromDrafts(templateState.fileNameSlotDrafts)
    val isIncluded = derivedFileNameSlots.contains(cell.cellId)
    var isSlotEditMode by remember { mutableStateOf(false) }
    var selectedFromIndex by remember { mutableStateOf<Int?>(null) }
    var isCounterScopeDialogOpen by remember { mutableStateOf(false) }
    var pendingCounterScopeMode by remember {
        mutableStateOf(cell.counterScopeMode ?: CounterScopeMode.EXCLUDE)
    }
    val panelScrollState = rememberScrollState()

    val configuration = LocalConfiguration.current
    val maxPanelHeight = configuration.screenHeightDp.dp * 0.5f
    val panelVerticalPadding = if (compactForBottomPanel) 6.dp else 14.dp
    val sectionSpacing = if (compactForBottomPanel) 6.dp else 12.dp
    val buttonHeight = if (compactForBottomPanel) 36.dp else 44.dp
    // 주요 정책: CELL_EDIT compact에서는 터치 가능 크기를 유지한 범위에서 밀도를 한 단계 높인다.


    androidx.compose.runtime.LaunchedEffect(cell.cellId, cell.counterScopeMode) {
        pendingCounterScopeMode = cell.counterScopeMode ?: CounterScopeMode.EXCLUDE
    }

    val hasDialogPendingPreview = isCounterScopeDialogOpen

    // 패널 미리보기는 저장(확정) 이전에도 현재 다이얼로그에서 선택 중인 값을 즉시 반영한다.
    // 단, 취소 시에는 원본 셀 상태로 되돌아가야 하므로 "다이얼로그가 열려있는 동안"에만 pending 값을 합성한다.
    val previewSelectedCell = remember(
        cell,
        hasDialogPendingPreview,
        isCounterScopeDialogOpen,
        pendingCounterScopeMode
    ) {
        var previewCell = cell
        if (isCounterScopeDialogOpen && (previewCell.dataType == TableCellDataType.DATE || previewCell.dataType == TableCellDataType.TIME)) {
            previewCell = previewCell.copy(counterScopeMode = pendingCounterScopeMode)
        }
        previewCell
    }

    // resolver 입력도 선택 셀의 최신 편집 스냅샷으로 맞춰 stale preview를 방지한다.
    val previewCells = remember(templateState.cells, previewSelectedCell) {
        templateState.cells.map { originalCell ->
            if (originalCell.cellId == previewSelectedCell.cellId) previewSelectedCell else originalCell
        }
    }

    val resolver = remember { TableResolver() }
    val selectedPhraseTextByCellId = remember(previewCells, phraseSets, phraseProgressCursor) {
        PhraseResolver.resolveSelectedTextByCellId(
            cells = previewCells,
            phraseSets = phraseSets,
            // 문구 선택은 문구 진행 커서 기반으로 고정한다(카운터 seed 사용 금지).
            progressCursor = phraseProgressCursor,
        )
    }

    val panelResolvedByCellId = remember(
        resolver,
        previewCells,
        selectedPhraseTextByCellId,
        previewNow,
        previewCounterDigits,
        scopeNextCounter,
        dateFormat,
        timeFormat,
        phraseSets
    ) {
        resolver.plan(
            cells = previewCells,
            captureNow = previewNow,
            config = TableResolver.Config(
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            counterSeedOverride = scopeNextCounter,
            selectedPhraseTextByCellId = selectedPhraseTextByCellId,
        ).resolvedCells.associate { it.id to it.resolvedText }
    }

    // 상단 프리뷰와 패널 프리뷰 기준을 최대한 맞추기 위해,
    // pending 다이얼로그가 없을 때는 부모(TableEditorScreen)의 resolved snapshot을 그대로 사용한다.
    val resolvedByCellId = if (hasDialogPendingPreview) panelResolvedByCellId else baseResolvedByCellId

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxPanelHeight)
            .let { base ->
                // 정책 보강: CELL_EDIT 하단 패널 내부에서는 외곽 카드 중복을 제거해 밀도를 높인다.
                if (compactForBottomPanel) base else base.background(DDZColor.Surface, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            }
            .padding(horizontal = panelVerticalPadding, vertical = panelVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(if (compactForBottomPanel) 4.dp else 10.dp)
    ) {
        if (!compactForBottomPanel) {
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
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(panelScrollState),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            if (showPathGroupSection) {
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
            }

            if (showFileNameSection) {
                Column(verticalArrangement = Arrangement.spacedBy(if (compactForBottomPanel) 6.dp else 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("파일명", style = DDZTypography.Body, color = DDZColor.TextMuted)
                    Button(
                        modifier = Modifier.height(if (compactForBottomPanel) 36.dp else 30.dp),
                        onClick = {
                            val enable = !isIncluded
                            if (enable) {
                                val currentSlotCount = derivedFileNameSlots.count { it != null }
                                if (currentSlotCount >= FILE_NAME_SLOT_COUNT) {
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
                            val slotCellId = derivedFileNameSlots.getOrNull(index)
                            val slotText = when {
                                slotCellId == null -> "비어있음"
                                else -> resolvedByCellId[slotCellId].orEmpty().ifBlank { "(값 없음)" }
                            }
                            val isSelected = slotCellId == cell.cellId
                            val isFromSelected = isSlotEditMode && selectedFromIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (compactForBottomPanel) 36.dp else 44.dp)
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
                                            onLongPress = { _: Offset ->
                                                isSlotEditMode = !isSlotEditMode
                                                selectedFromIndex = null
                                            },
                                            onTap = { _: Offset ->
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

            // 주요 정책: 데이터 타입/타입별 설정 UI는 파일명/경로 섹션 노출 여부와 무관하게 항상 렌더한다.
            }

            Spacer(Modifier.height(if (compactForBottomPanel) 0.dp else 2.dp))

            if (cell.dataType == TableCellDataType.COUNTER && onResetCounterSeed != null) {
                SectionCaption("카운터")
                Spacer(Modifier.height(4.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().height(buttonHeight),
                    onClick = onResetCounterSeed
                ) {
                    Text("동기화 ($autoNextCounterValue)", style = DDZTypography.ButtonText)
                }
            }

            if (cell.dataType == TableCellDataType.DATE || cell.dataType == TableCellDataType.TIME) {
                val formatLabel = if (cell.dataType == TableCellDataType.DATE) "날짜 형식" else "시간 형식"
                val effectiveCounterScopeMode = if (isCounterScopeDialogOpen) {
                    pendingCounterScopeMode
                } else {
                    cell.counterScopeMode ?: CounterScopeMode.EXCLUDE
                }
                val scopeLabel = when (effectiveCounterScopeMode) {
                    CounterScopeMode.EXCLUDE -> "스코프: 제외"
                    CounterScopeMode.INCLUDE -> "스코프: 포함"
                }

                SectionCaption("형식 설정")
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        onClick = { onOpenFormatDialog(cell.cellId, cell.dataType) }
                    ) {
                        Text(formatLabel, style = DDZTypography.ButtonText)
                    }
                    Button(
                        modifier = Modifier.weight(1f).height(buttonHeight),
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
                    containerColor = DDZColor.Surface,
                    onDismissRequest = { isCounterScopeDialogOpen = false },
                    title = { Text("카운터 스코프", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(if (compactForBottomPanel) 6.dp else 8.dp)) {
                            Text(
                                text = "이 날짜/시간 값이 바뀌면 카운터도 분리됩니다.",
                                style = DDZTypography.Body,
                                color = DDZColor.TextPrimary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pendingCounterScopeMode = CounterScopeMode.EXCLUDE },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = pendingCounterScopeMode == CounterScopeMode.EXCLUDE,
                                    onClick = { pendingCounterScopeMode = CounterScopeMode.EXCLUDE },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = DDZColor.Primary,
                                        unselectedColor = DDZColor.TextMuted
                                    )
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
                                    onClick = { pendingCounterScopeMode = CounterScopeMode.INCLUDE },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = DDZColor.Primary,
                                        unselectedColor = DDZColor.TextMuted
                                    )
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
                            Text("확인", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isCounterScopeDialogOpen = false }) {
                            Text("취소", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                        }
                    }
                )
            }

            if (cell.dataType == TableCellDataType.ROTATING_TEXT && onOpenRotatingTemplateDialog != null) {
                SectionCaption("순환문구")
                Spacer(Modifier.height(4.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    onClick = onOpenRotatingTemplateDialog
                ) {
                    Text("템플릿 설정", style = DDZTypography.ButtonText)
                }
            }

            // 주요 정책: CELL_EDIT 본문 순서는 타입별 버튼 다음에 데이터 타입 카드를 배치한다.
            SectionCaption("데이터 타입")
            Spacer(Modifier.height(4.dp))
            DataTypeCardGrid3(
                selected = cell.dataType,
                onSelect = onSetDataType,
                compact = compactForBottomPanel
            )

            Spacer(Modifier.height(if (compactForBottomPanel) 2.dp else 10.dp))
        }
    }
}

@Composable
private fun SectionCaption(
    text: String
) {
    Text(
        text = text,
        style = DDZTypography.Caption,
        color = DDZColor.TextMuted
    )
}

@Composable
private fun DataTypeCardGrid3(
    selected: TableCellDataType,
    onSelect: (TableCellDataType) -> Unit,
    compact: Boolean = false
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
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
                row.forEach { (type, icon, koLabel) ->
                    val isSelected = selected == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 56.dp else 74.dp)
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
                            .padding(if (compact) 6.dp else 10.dp)
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
                                modifier = Modifier.height(if (compact) 20.dp else 30.dp)
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
