package com.example.dzlog.ui.camera.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [CaptureAspectSection]
 * - 목적: 촬영 비율 선택 UI
 */
@Composable
internal fun CaptureAspectSection(
    value: CaptureAspect,
    onChange: (CaptureAspect) -> Unit
) {
    Text("촬영 비율", style = DDZTypography.Body, color = DDZColor.Surface)

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = (value == CaptureAspect.R3_4), onClick = { onChange(CaptureAspect.R3_4) })
        Text("3:4", style = DDZTypography.Body, color = DDZColor.Surface)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = (value == CaptureAspect.R9_16), onClick = { onChange(CaptureAspect.R9_16) })
        Text("9:16", style = DDZTypography.Body, color = DDZColor.Surface)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = (value == CaptureAspect.R1_1), onClick = { onChange(CaptureAspect.R1_1) })
        Text("1:1", style = DDZTypography.Body, color = DDZColor.Surface)
    }

    Spacer(Modifier.height(DDZSpacing.screenPadding))
}
