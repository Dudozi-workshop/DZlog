package com.example.dzlog.ui.camera.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [ContinuousPreviewSection]
 * - 목적: 연속 촬영 미리보기 모드 선택 UI
 */
@Composable
internal fun ContinuousPreviewSection(
    value: ContinuousPreviewMode,
    onChange: (ContinuousPreviewMode) -> Unit
) {
    Text("연속 촬영 미리보기", style = DDZTypography.Body, color = DDZColor.Surface)
    Row(verticalAlignment = Alignment.CenterVertically) {
        ContinuousPreviewMode.entries.forEach { mode ->
            RadioButton(
                selected = value == mode,
                onClick = { onChange(mode) }
            )
            Text(mode.name, style = DDZTypography.Body, color = DDZColor.Surface)
            Spacer(Modifier.width(8.dp))
        }
    }
}
