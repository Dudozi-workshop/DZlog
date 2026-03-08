package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun BottomEditorPanel(
    panelMode: BottomEditorPanelMode,
    rows: Int,
    cols: Int,
    isSaving: Boolean,
    filenamePreview: String,
    savePathPreview: String,
    selectedCellLabel: String?,
    fileNameSlotItems: List<FileNameSlotUiItem?>,
    selectedFileNameSlot: Int?,
    modifier: Modifier = Modifier,
    onSelectFileNameSlot: (Int) -> Unit,
    onFillEmptyFileNameSlot: (Int) -> Unit,
    onMoveSelectedFileNameSlotLeft: () -> Unit,
    onMoveSelectedFileNameSlotRight: () -> Unit,
    onDeleteSelectedFileNameSlot: () -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: () -> Unit,
    onAddCol: () -> Unit,
    onRemoveCol: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (panelMode) {
            BottomEditorPanelMode.NONE -> {
                Text(
                    text = "셀을 눌러 편집을 시작하세요.",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted
                )
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
                        } else {
                            Text("저장", style = DDZTypography.ButtonText)
                        }
                    }
                }
            }

            BottomEditorPanelMode.CELL_EDIT -> {
                Text("셀 편집", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Text(
                    text = selectedCellLabel?.let { "선택 셀: $it" } ?: "선택된 셀이 없습니다.",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted
                )
                Text(
                    text = "다음 단계에서 셀 타입/값 편집 UI가 들어올 자리입니다.",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted
                )
            }

            BottomEditorPanelMode.FILENAME_EDIT -> {
                val normalizedSlots = List(3) { index -> fileNameSlotItems.getOrNull(index) }
                val previewLabel = normalizedSlots.mapNotNull { it?.label }.joinToString("_")
                val selectedSlotIsFilled = selectedFileNameSlot?.let { normalizedSlots.getOrNull(it) != null } == true
                // 정책 변경: 이동 가능 여부는 filledCount가 아니라 "인접 슬롯이 실제로 채워졌는지"로 판정한다.
                val canMoveLeft = selectedFileNameSlot?.let { index ->
                    selectedSlotIsFilled && normalizedSlots.getOrNull(index - 1) != null
                } == true
                val canMoveRight = selectedFileNameSlot?.let { index ->
                    selectedSlotIsFilled && normalizedSlots.getOrNull(index + 1) != null
                } == true
                val canDelete = selectedSlotIsFilled

                Text("파일명 구성 편집", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    normalizedSlots.forEachIndexed { index, slot ->
                        val isSelected = selectedFileNameSlot == index
                        val slotLabel = slot?.label ?: "+"
                        val slotTextColor = if (slot == null) DDZColor.TextMuted else DDZColor.TextPrimary

                        // 정책 변경: FILENAME_EDIT에서는 3슬롯 고정 UI를 우선 제공하고,
                        // 빈 슬롯 클릭 시 임시 요소("셀")를 채워 다음 단계 상세 선택 UI로 연결한다.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) DDZColor.PrimaryDark.copy(alpha = 0.14f) else DDZColor.Background,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) DDZColor.Primary else DDZColor.Card,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSelectFileNameSlot(index)
                                    if (slot == null) onFillEmptyFileNameSlot(index)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(slotLabel, style = DDZTypography.Caption, color = slotTextColor)
                        }
                    }
                }

                Text("선택된 요소 조정", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedFileNameSlotLeft, enabled = canMoveLeft) {
                        Text("← 이동", style = DDZTypography.ButtonText)
                    }
                    Button(modifier = Modifier.weight(1f), onClick = onMoveSelectedFileNameSlotRight, enabled = canMoveRight) {
                        Text("→ 이동", style = DDZTypography.ButtonText)
                    }
                    Button(modifier = Modifier.weight(1f), onClick = onDeleteSelectedFileNameSlot, enabled = canDelete) {
                        Text("삭제", style = DDZTypography.ButtonText)
                    }
                }

                Text("프리뷰", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Text(
                    text = if (previewLabel.isBlank()) "기본 파일명 사용" else previewLabel,
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary
                )
            }

            BottomEditorPanelMode.PATH_EDIT -> {
                Text("저장경로 구성 편집", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Text("현재 프리뷰: $savePathPreview", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Text("다음 단계에서 슬롯 UI가 들어올 자리입니다.", style = DDZTypography.Caption, color = DDZColor.TextMuted)
            }
        }
    }
}
