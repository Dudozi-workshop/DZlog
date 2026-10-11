package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.SaveMode
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
    counterPadding: Int,
    nextCounter: Int,
    onOpenDetail: (TableEditorSaveDetail) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DDZColor.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TableEditorSettingRow(
            label = "파일명",
            leadingIcon = Icons.Filled.TextFields,
            value = fileNamePreview,
            onClick = { onOpenDetail(TableEditorSaveDetail.FILE_NAME) },
        )
        TableEditorSettingRow(
            label = "저장 위치",
            leadingIcon = Icons.Filled.Folder,
            value = pathPreview,
            onClick = { onOpenDetail(TableEditorSaveDetail.SAVE_PATH) },
        )
        TableEditorSettingRow(
            label = "자동번호",
            leadingIcon = Icons.Filled.Tag,
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


