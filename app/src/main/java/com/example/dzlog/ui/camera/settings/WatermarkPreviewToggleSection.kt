package com.example.dzlog.ui.camera.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [WatermarkPreviewToggleSection]
 * - 목적: 촬영 화면 워터마크 미리보기 표시 여부 토글 UI
 */
@Composable
internal fun WatermarkPreviewToggleSection(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            "촬영 화면에 워터마크 미리보기 표시",
            style = DDZTypography.Body,
            color = DDZColor.Surface
        )
    }
}
