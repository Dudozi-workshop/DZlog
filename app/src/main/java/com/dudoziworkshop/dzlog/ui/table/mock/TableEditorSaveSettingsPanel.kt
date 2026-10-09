package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.ui.common.DDZSettingRow
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

internal enum class TableEditorSaveDetail {
    FILE_NAME,
    SAVE_PATH,
    SAVE_MODE,
    AUTO_NUMBER,
    AUTO_NUMBER_ADVANCED,
}

@Composable
internal fun TableEditorSaveSettingsPanel(
    draft: MockSaveRulesDraft,
    fileNamePreview: String,
    pathPreview: String,
    saveMode: SaveMode,
    counterPadding: Int,
    nextCounter: Int,
    onOpenDetail: (TableEditorSaveDetail) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Surface)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DDZSettingRow(
            label = "파일명",
            value = fileNamePreview,
            onClick = { onOpenDetail(TableEditorSaveDetail.FILE_NAME) },
        )
        DDZSettingRow(
            label = "저장 위치",
            value = pathPreview,
            onClick = { onOpenDetail(TableEditorSaveDetail.SAVE_PATH) },
        )
        DDZSettingRow(
            label = "저장 방식",
            value = saveModeLabel(saveMode),
            onClick = { onOpenDetail(TableEditorSaveDetail.SAVE_MODE) },
        )
        DDZSettingRow(
            label = "자동번호",
            value = "다음 ${formatMockCounter(nextCounter, counterPadding)} · ${counterPaddingLabel(counterPadding)}",
            onClick = { onOpenDetail(TableEditorSaveDetail.AUTO_NUMBER) },
        )
    }
}

internal fun saveModeLabel(mode: SaveMode): String =
    when (mode) {
        SaveMode.WATERMARK_ONLY -> "결과사진만"
        SaveMode.ORIGINAL_ONLY -> "원본만"
        SaveMode.BOTH -> "원본 + 결과사진"
    }

internal fun counterPaddingLabel(counterPadding: Int): String =
    "${counterPadding.coerceIn(1, 4)}자리"

internal fun formatMockCounter(value: Int, counterPadding: Int): String {
    val normalized = value.coerceAtLeast(1).toString()
    return if (counterPadding > 0) {
        normalized.padStart(counterPadding.coerceIn(1, 4), '0')
    } else {
        normalized
    }
}

