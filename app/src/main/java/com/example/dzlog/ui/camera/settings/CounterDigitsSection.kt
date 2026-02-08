package com.example.dzlog.ui.camera.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [CounterDigitsSection]
 * - 목적: 카운터 자릿수 조정 UI
 */
@Composable
internal fun CounterDigitsSection(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Text("카운터 자릿수", style = DDZTypography.Body, color = DDZColor.Surface)
    Text(
        "예: 4자리면 0001",
        color = DDZColor.Surface.copy(alpha = 0.7f),
        style = DDZTypography.Caption
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onMinus) { Text("-", style = DDZTypography.ButtonText, color = DDZColor.Surface) }
        Spacer(Modifier.width(DDZSpacing.sectionGap))
        Text(value.toString(), style = DDZTypography.CardTitle, color = DDZColor.Surface)
        Spacer(Modifier.width(DDZSpacing.sectionGap))
        Button(onClick = onPlus) { Text("+", style = DDZTypography.ButtonText, color = DDZColor.Surface) }
    }
}
