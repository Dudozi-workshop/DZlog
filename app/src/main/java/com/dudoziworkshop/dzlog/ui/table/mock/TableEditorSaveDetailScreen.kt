package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.naming.NamingSlotPreview
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZSettingRow
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun TableEditorSaveDetailScreen(
    detail: TableEditorSaveDetail,
    draft: MockSaveRulesDraft,
    fileNamePreview: String,
    pathPreview: String,
    saveMode: SaveMode,
    counterPadding: Int,
    nextCounter: Int,
    cells: List<TableEditorCellUiModel>,
    rows: Int,
    cols: Int,
    isCounterBusy: Boolean,
    counterStatus: String?,
    onBack: () -> Unit,
    onOpenDetail: (TableEditorSaveDetail) -> Unit,
    onSaveModeChange: (SaveMode) -> Unit,
    onCounterPaddingChange: (Int) -> Unit,
    onNextCounterChange: (Int) -> Unit,
    onSyncCounter: () -> Unit,
    onResetCounter: () -> Unit,
    onManualPreview: (Boolean, Int, String) -> NamingSlotPreview,
    onDraftChange: (MockSaveRulesDraft) -> Unit,
) {
    var showCounterInput by remember { mutableStateOf(false) }
    var counterInputDraft by remember(nextCounter) { mutableStateOf(nextCounter.toString()) }

    val scrollState = key(detail) { rememberScrollState() }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            DDZTopBar(
                title = when (detail) {
                    TableEditorSaveDetail.FILE_NAME -> "파일명 설정"
                    TableEditorSaveDetail.SAVE_PATH -> "저장 위치 설정"
                    TableEditorSaveDetail.SAVE_MODE -> "저장 방식"
                    TableEditorSaveDetail.AUTO_NUMBER -> "자동번호 설정"
                    TableEditorSaveDetail.AUTO_NUMBER_ADVANCED -> "자동번호 고급 옵션"
                },
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (detail) {
                TableEditorSaveDetail.FILE_NAME -> {
                    PreviewCard(
                        label = "현재 파일명",
                        value = fileNamePreview,
                    )
                    Text("파일명 구성", style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary)
                    TableEditorRuleListEditor(
                        isFileName = true,
                        items = draft.fileNameItems,
                        cells = cells,
                        rows = rows,
                        cols = cols,
                        onManualPreview = onManualPreview,
                        onItemsChange = { onDraftChange(draft.copy(fileNameItems = it)) },
                    )
                    DDZSettingRow(
                        label = "자동번호",
                        value = formatMockCounter(nextCounter, counterPadding),
                        onClick = { onOpenDetail(TableEditorSaveDetail.AUTO_NUMBER) },
                    )
                }

                TableEditorSaveDetail.SAVE_PATH -> {
                    PreviewCard(
                        label = "현재 저장 위치",
                        value = pathPreview,
                    )
                    Text("폴더 구성", style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary)
                    TableEditorRuleListEditor(
                        isFileName = false,
                        items = draft.pathItems,
                        cells = cells,
                        rows = rows,
                        cols = cols,
                        onManualPreview = onManualPreview,
                        onItemsChange = { onDraftChange(draft.copy(pathItems = it)) },
                    )
                    if (draft.pathItems.all { it == null }) {
                        Text(
                            "추가 폴더가 없으면 Pictures/DZlog/에 저장합니다.",
                            style = DDZTypography.Secondary,
                            color = DDZColor.TextSecondary,
                        )
                    }
                }

                TableEditorSaveDetail.SAVE_MODE -> {
                    Text(
                        "저장할 이미지",
                        style = DDZTypography.SectionTitle,
                        color = DDZColor.TextPrimary,
                    )
                    SaveModeOption(
                        title = "결과사진만",
                        description = "표가 포함된 최종 이미지만 저장합니다.",
                        selected = saveMode == SaveMode.WATERMARK_ONLY,
                        onClick = { onSaveModeChange(SaveMode.WATERMARK_ONLY) },
                    )
                    SaveModeOption(
                        title = "원본만",
                        description = "촬영 원본 이미지만 저장합니다.",
                        selected = saveMode == SaveMode.ORIGINAL_ONLY,
                        onClick = { onSaveModeChange(SaveMode.ORIGINAL_ONLY) },
                    )
                    SaveModeOption(
                        title = "원본 + 결과사진",
                        description = "원본과 표가 포함된 이미지를 모두 저장합니다.",
                        selected = saveMode == SaveMode.BOTH,
                        onClick = { onSaveModeChange(SaveMode.BOTH) },
                    )
                }

                TableEditorSaveDetail.AUTO_NUMBER -> {
                    Text("다음 번호", style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DDZButton(
                            text = "−",
                            style = DDZButtonStyle.Secondary,
                            enabled = !isCounterBusy,
                            onClick = { onNextCounterChange((nextCounter - 1).coerceAtLeast(1)) },
                        )
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isCounterBusy) {
                                    counterInputDraft = nextCounter.toString()
                                    showCounterInput = true
                                },
                            color = DDZColor.Surface,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DDZColor.Border),
                        ) {
                            Text(
                                text = formatMockCounter(nextCounter, counterPadding),
                                modifier = Modifier.padding(vertical = 14.dp),
                                style = DDZTypography.SectionTitle,
                                color = DDZColor.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                        DDZButton(
                            text = "+",
                            style = DDZButtonStyle.Secondary,
                            enabled = !isCounterBusy,
                            onClick = { onNextCounterChange(nextCounter + 1) },
                        )
                    }

                    Text("자릿수", style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..4).forEach { digits ->
                            DDZButton(
                                text = digits.toString(),
                                modifier = Modifier.weight(1f),
                                minHeight = 48.dp,
                                style = DDZButtonStyle.Secondary,
                                containerColorOverride = if (counterPadding.coerceIn(1, 4) == digits) DDZColor.SelectedSoft else null,
                                onClick = { onCounterPaddingChange(digits) },
                            )
                        }
                    }

                    PreviewCard(
                        label = "미리보기",
                        value = formatMockCounter(nextCounter, counterPadding),
                    )

                    DDZSettingRow(
                        label = "번호 동기화",
                        value = "저장된 사진의 마지막 번호 다음 값으로 맞춥니다.",
                        onClick = if (isCounterBusy) null else onSyncCounter,
                    )
                    if (!counterStatus.isNullOrBlank()) {
                        Text(
                            counterStatus,
                            style = DDZTypography.Secondary,
                            color = DDZColor.SelectedDark,
                        )
                    }

                    DDZSettingRow(
                        label = "고급 옵션",
                        value = "번호 구분 기준",
                        onClick = { onOpenDetail(TableEditorSaveDetail.AUTO_NUMBER_ADVANCED) },
                    )

                    DDZButton(
                        text = "번호 초기화",
                        modifier = Modifier.fillMaxWidth(),
                        style = DDZButtonStyle.Destructive,
                        enabled = !isCounterBusy,
                        onClick = onResetCounter,
                    )
                }

                TableEditorSaveDetail.AUTO_NUMBER_ADVANCED -> {
                    Text("번호 구분 기준", style = DDZTypography.SectionTitle, color = DDZColor.TextPrimary)
                    ScopeOption(
                        title = "저장 위치별 번호 분리",
                        description = "폴더가 다르면 번호를 따로 사용합니다.",
                        checked = draft.includePathInScope,
                        onCheckedChange = {
                            onDraftChange(draft.copy(includePathInScope = it))
                        },
                    )
                    ScopeOption(
                        title = "파일명 구성별 번호 분리",
                        description = "파일명 규칙이 다르면 번호를 따로 사용합니다.",
                        checked = draft.includeFilenameInScope,
                        onCheckedChange = {
                            onDraftChange(draft.copy(includeFilenameInScope = it))
                        },
                    )
                }
            }
        }
    }

    if (showCounterInput) {
        DDZBottomSheet(
            title = "다음 번호 직접 입력",
            onDismiss = { showCounterInput = false },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DDZTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = counterInputDraft,
                    onValueChange = { input ->
                        counterInputDraft = input.filter(Char::isDigit).take(7)
                    },
                    label = "다음 번호",
                )
                DDZButton(
                    text = "확인",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = counterInputDraft.toIntOrNull()?.let { it >= 1 } == true,
                    onClick = {
                        val next = counterInputDraft.toIntOrNull()?.coerceAtLeast(1)
                            ?: return@DDZButton
                        onNextCounterChange(next)
                        showCounterInput = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.SurfaceSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        Text(value, style = DDZTypography.Body, color = DDZColor.TextPrimary)
    }
}

@Composable
private fun SaveModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) DDZColor.SelectedSoft else DDZColor.Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) DDZColor.Selected else DDZColor.Border,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
                Text(description, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
        }
    }
}

@Composable
private fun ScopeOption(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(title, style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            Text(description, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


