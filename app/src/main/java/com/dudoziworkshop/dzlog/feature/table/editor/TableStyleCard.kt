package com.dudoziworkshop.dzlog.feature.table.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

@Composable
fun TableStyleCard(
    style: TableStyleState,
    onBgStyleChange: (Int) -> Unit,
    onGridEnabledChange: (Boolean) -> Unit,
    onTextColorModeChange: (Int) -> Unit,
    onManualTextColorChange: (Int) -> Unit,
    onValueScaleChange: (Int) -> Unit,
    onTextAlignChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("표 서식설정")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onBgStyleChange(0) }) { Text("배경 검정") }
            Button(onClick = { onBgStyleChange(1) }) { Text("배경 흰색") }
            Button(onClick = { onBgStyleChange(2) }) { Text("배경 투명") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("격자")
            Switch(checked = style.gridEnabled, onCheckedChange = onGridEnabledChange)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onTextColorModeChange(0) }) { Text("자동색") }
            Button(onClick = { onTextColorModeChange(1) }) { Text("수동색") }
            Button(onClick = { onManualTextColorChange(0) }) { Text("흰글씨") }
            Button(onClick = { onManualTextColorChange(1) }) { Text("검은글씨") }
        }
        Text("글씨 크기: ${style.valueScale}")
        Slider(value = style.valueScale.toFloat(), onValueChange = { onValueScaleChange(it.toInt()) }, valueRange = 60f..160f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onTextAlignChange(0) }) { Text("좌") }
            Button(onClick = { onTextAlignChange(1) }) { Text("중") }
            Button(onClick = { onTextAlignChange(2) }) { Text("우") }
        }
    }
}
