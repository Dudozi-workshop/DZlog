package com.example.dzlog.ui.camera.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [SaveModeSection]
 * - 목적: 저장 모드 선택 UI
 */
@Composable
internal fun SaveModeSection(
    value: SaveMode,
    onChange: (SaveMode) -> Unit
) {
    Text("저장 모드", style = DDZTypography.Body, color = DDZColor.Surface)
    Spacer(Modifier.height(DDZSpacing.itemGap))

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = value == SaveMode.WATERMARK_ONLY, onClick = { onChange(SaveMode.WATERMARK_ONLY) })
        Text("워터마크", style = DDZTypography.Body, color = DDZColor.Surface)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = value == SaveMode.ORIGINAL_ONLY, onClick = { onChange(SaveMode.ORIGINAL_ONLY) })
        Text("원본", style = DDZTypography.Body, color = DDZColor.Surface)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = value == SaveMode.BOTH, onClick = { onChange(SaveMode.BOTH) })
        Text("원본+워터마크", style = DDZTypography.Body, color = DDZColor.Surface)
    }

    Spacer(Modifier.height(DDZSpacing.sectionGap + DDZSpacing.itemGap))
}
