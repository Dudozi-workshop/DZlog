package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode
import com.dudoziworkshop.dzlog.ui.table.section.FileNameFormatType
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathFormatType
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun BottomEditorPanel(
    panelMode: BottomEditorPanelMode,
    rows: Int,
    cols: Int,
    isSaving: Boolean,
    selectedCellLabel: String?,
    fileNameSlotItems: List<FileNameSlotUiItem?>,
    selectedFileNameSlot: Int?,
    isFileNameCellPickMode: Boolean,
    showFileNameFormatOptions: Boolean,
    manualInputDraft: String,
    showManualInputEditor: Boolean,
    pathSlotItems: List<PathSlotUiItem?>,
    selectedPathSlot: Int?,
    isPathCellPickMode: Boolean,
    showPathFormatOptions: Boolean,
    showPathManualInputEditor: Boolean,
    pathManualInputDraft: String,
    modifier: Modifier = Modifier,
    onSelectFileNameSlot: (Int) -> Unit,
    onFillEmptyFileNameSlot: (Int) -> Unit,
    onMoveSelectedFileNameSlotLeft: () -> Unit,
    onMoveSelectedFileNameSlotRight: () -> Unit,
    onDeleteSelectedFileNameSlot: () -> Unit,
    onStartFileNameCellPick: () -> Unit,
    onToggleFileNameFormatOptions: () -> Unit,
    onApplyFileNameFormatType: (FileNameFormatType) -> Unit,
    onStartManualInputEditor: () -> Unit,
    onManualInputDraftChange: (String) -> Unit,
    onApplyManualInput: () -> Unit,
    onSelectPathSlot: (Int) -> Unit,
    onFillEmptyPathSlot: (Int) -> Unit,
    onMoveSelectedPathSlotLeft: () -> Unit,
    onMoveSelectedPathSlotRight: () -> Unit,
    onDeleteSelectedPathSlot: () -> Unit,
    onStartPathCellPick: () -> Unit,
    onTogglePathFormatOptions: () -> Unit,
    onApplyPathFormatType: (PathFormatType) -> Unit,
    onStartPathManualInputEditor: () -> Unit,
    onPathManualInputDraftChange: (String) -> Unit,
    onApplyPathManualInput: () -> Unit,
    onClosePanel: () -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: () -> Unit,
    onAddCol: () -> Unit,
    onRemoveCol: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    @Composable
    fun PanelHeader(title: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = DDZTypography.Body, color = DDZColor.TextPrimary)
            IconButton(onClick = onClosePanel, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "패널 닫기",
                    tint = DDZColor.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (panelMode) {
            BottomEditorPanelMode.NONE -> {
                Text("셀을 눌러 편집을 시작하세요.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onAddRow) { Text("행 +", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onRemoveRow, enabled = rows > 1) { Text("행 -", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onAddCol) { Text("열 +", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onRemoveCol, enabled = cols > 1) { Text("열 -", style = DDZTypography.ButtonText) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onReset) { Text("초기화", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onSave, enabled = !isSaving) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("저장 중...", style = DDZTypography.ButtonText)
                        } else Text("저장", style = DDZTypography.ButtonText)
                    }
                }
            }
            BottomEditorPanelMode.CELL_EDIT -> {
                PanelHeader("셀 편집")
                Text(selectedCellLabel?.let { "선택 셀: $it" } ?: "선택된 셀이 없습니다.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Text("다음 단계에서 셀 타입/값 편집 UI가 들어올 자리입니다.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
            }
            BottomEditorPanelMode.FILENAME_EDIT -> {
                val normalizedSlots = List(3) { index -> fileNameSlotItems.getOrNull(index) }
                val selectedSlotIsFilled = selectedFileNameSlot?.let { normalizedSlots.getOrNull(it) != null } == true
                val canMoveLeft = selectedFileNameSlot?.let { selectedSlotIsFilled && normalizedSlots.getOrNull(it - 1) != null } == true
                val canMoveRight = selectedFileNameSlot?.let { selectedSlotIsFilled && normalizedSlots.getOrNull(it + 1) != null } == true
                val canDelete = selectedSlotIsFilled

                PanelHeader("파일명 구성 편집")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    normalizedSlots.forEachIndexed { index, slot ->
                        val isSelected = selectedFileNameSlot == index
                        Box(
                            modifier = Modifier.weight(1f)
                                .background(if (isSelected) DDZColor.PrimaryDark.copy(alpha = 0.14f) else DDZColor.Background, RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) DDZColor.Primary else DDZColor.Card, RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectFileNameSlot(index)
                                    if (slot == null) onFillEmptyFileNameSlot(index)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(slot?.label ?: "+", style = DDZTypography.Caption, color = if (slot == null) DDZColor.TextMuted else DDZColor.TextPrimary) }
                    }
                }

                Text("선택된 요소 조정", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedFileNameSlotLeft, enabled = canMoveLeft) { Text("← 이동", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedFileNameSlotRight, enabled = canMoveRight) { Text("→ 이동", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onDeleteSelectedFileNameSlot, enabled = canDelete) { Text("삭제", style = DDZTypography.ButtonText) }
                }

                val hasSelectedSlot = selectedFileNameSlot != null
                Text("요소 선택", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onStartFileNameCellPick, enabled = hasSelectedSlot) { Text("셀", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onToggleFileNameFormatOptions, enabled = hasSelectedSlot) { Text("서식", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onStartManualInputEditor, enabled = hasSelectedSlot) { Text("직접입력", style = DDZTypography.ButtonText) }
                }

                if (showFileNameFormatOptions && hasSelectedSlot) {
                    Text("서식 선택", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(modifier = Modifier.weight(1f), onClick = { onApplyFileNameFormatType(FileNameFormatType.DATE) }) { Text("날짜", style = DDZTypography.ButtonText) }
                        Button(modifier = Modifier.weight(1f), onClick = { onApplyFileNameFormatType(FileNameFormatType.TIME) }) { Text("시간", style = DDZTypography.ButtonText) }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(modifier = Modifier.weight(1f), onClick = { onApplyFileNameFormatType(FileNameFormatType.COUNTER) }) { Text("카운터", style = DDZTypography.ButtonText) }
                        Button(modifier = Modifier.weight(1f), onClick = { onApplyFileNameFormatType(FileNameFormatType.ROTATING_TEXT) }) { Text("순환문구", style = DDZTypography.ButtonText) }
                    }
                }

                if (showManualInputEditor && hasSelectedSlot) {
                    Text("직접입력", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = manualInputDraft, onValueChange = onManualInputDraftChange, singleLine = true, placeholder = { Text("텍스트를 입력하세요") })
                    Button(onClick = onApplyManualInput, enabled = manualInputDraft.trim().isNotBlank()) { Text("적용", style = DDZTypography.ButtonText) }
                }

                if (isFileNameCellPickMode && hasSelectedSlot) {
                    Text("셀 선택 대기 중: 위 표에서 셀을 탭하면 현재 슬롯에 연결됩니다.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }

                // 정책 변경: 파일명 최종 프리뷰는 상단 CompactPathHeader를 단일 소스로 사용한다.
                // 하단 패널은 편집 도구(슬롯/서식/직접입력)만 담당한다.
            }
            BottomEditorPanelMode.PATH_EDIT -> {
                val normalizedSlots = List(2) { index -> pathSlotItems.getOrNull(index) }
                val selectedPathFilled = selectedPathSlot?.let { normalizedSlots.getOrNull(it) != null } == true
                val canMoveLeft = selectedPathSlot?.let { selectedPathFilled && normalizedSlots.getOrNull(it - 1) != null } == true
                val canMoveRight = selectedPathSlot?.let { selectedPathFilled && normalizedSlots.getOrNull(it + 1) != null } == true
                val canDelete = selectedPathFilled
                val hasSelectedSlot = selectedPathSlot != null

                PanelHeader("저장경로 구성 편집")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    normalizedSlots.forEachIndexed { index, slot ->
                        val isSelected = selectedPathSlot == index
                        Box(
                            modifier = Modifier.weight(1f)
                                .background(if (isSelected) DDZColor.PrimaryDark.copy(alpha = 0.14f) else DDZColor.Background, RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) DDZColor.Primary else DDZColor.Card, RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectPathSlot(index)
                                    if (slot == null) onFillEmptyPathSlot(index)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(slot?.label ?: "+", style = DDZTypography.Caption, color = if (slot == null) DDZColor.TextMuted else DDZColor.TextPrimary) }
                    }
                }

                Text("선택된 요소 조정", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedPathSlotLeft, enabled = canMoveLeft) { Text("← 이동", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedPathSlotRight, enabled = canMoveRight) { Text("→ 이동", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onDeleteSelectedPathSlot, enabled = canDelete) { Text("삭제", style = DDZTypography.ButtonText) }
                }

                Text("요소 선택", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onStartPathCellPick, enabled = hasSelectedSlot) { Text("셀", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onTogglePathFormatOptions, enabled = hasSelectedSlot) { Text("서식", style = DDZTypography.ButtonText) }
                    Button(modifier = Modifier.weight(1f), onClick = onStartPathManualInputEditor, enabled = hasSelectedSlot) { Text("직접입력", style = DDZTypography.ButtonText) }
                }

                if (showPathFormatOptions && hasSelectedSlot) {
                    // 정책 변경: PATH 서식은 날짜/시간/순환문구만 허용하고 카운터는 제외한다.
                    Text("서식 선택", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(modifier = Modifier.weight(1f), onClick = { onApplyPathFormatType(PathFormatType.DATE) }) { Text("날짜", style = DDZTypography.ButtonText) }
                        Button(modifier = Modifier.weight(1f), onClick = { onApplyPathFormatType(PathFormatType.TIME) }) { Text("시간", style = DDZTypography.ButtonText) }
                        Button(modifier = Modifier.weight(1f), onClick = { onApplyPathFormatType(PathFormatType.ROTATING_TEXT) }) { Text("순환문구", style = DDZTypography.ButtonText) }
                    }
                }

                if (showPathManualInputEditor && hasSelectedSlot) {
                    Text("직접입력", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = pathManualInputDraft, onValueChange = onPathManualInputDraftChange, singleLine = true, placeholder = { Text("경로 텍스트를 입력하세요") })
                    Button(onClick = onApplyPathManualInput, enabled = pathManualInputDraft.trim().isNotBlank()) { Text("적용", style = DDZTypography.ButtonText) }
                }

                if (isPathCellPickMode && hasSelectedSlot) {
                    Text("셀 선택 대기 중: 위 표에서 셀을 탭하면 현재 경로 슬롯에 연결됩니다.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }

                // 정책 변경: 저장경로 최종 프리뷰도 상단 CompactPathHeader를 단일 소스로 사용한다.
            }
        }
    }
}
