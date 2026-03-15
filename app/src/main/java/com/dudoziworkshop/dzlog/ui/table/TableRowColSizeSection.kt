package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.Locale

private fun ensureRowWeights(state: TableTemplateState): List<Float> {
    val n = state.rows.coerceAtLeast(1)
    val w = state.rowWeights
    return if (w == null || w.size != n) List(n) { 1f } else w
}

private fun ensureColWeights(state: TableTemplateState): List<Float> {
    val n = state.cols.coerceAtLeast(1)
    val w = state.colWeights
    return if (w == null || w.size != n) List(n) { 1f } else w
}

@Composable
internal fun TableRowColSizeSection(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit
) {
    val rowWeights = ensureRowWeights(templateState)
    val colWeights = ensureColWeights(templateState)
    val editingRowWeights = remember { mutableStateListOf<Float>() }
    val editingColWeights = remember { mutableStateListOf<Float>() }
    val innerScroll = rememberScrollState()

    LaunchedEffect(rowWeights) {
        editingRowWeights.clear()
        editingRowWeights.addAll(rowWeights)
    }
    LaunchedEffect(colWeights) {
        editingColWeights.clear()
        editingColWeights.addAll(colWeights)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Card, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("행/열 크기(비율)", style = DDZTypography.CardTitle, color = DDZColor.TextMuted)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("균등 초기화", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            TextButton(onClick = {
                onTemplateChange(
                    templateState.copy(
                        rowWeights = List(templateState.rows.coerceAtLeast(1)) { 1f },
                        colWeights = List(templateState.cols.coerceAtLeast(1)) { 1f }
                    )
                )
            }) { Text("RESET", style = DDZTypography.ButtonText) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(innerScroll),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("행 높이", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            for (r in 0 until templateState.rows.coerceAtLeast(1)) {
                val v = editingRowWeights.getOrElse(r) { 1f }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R${r + 1}", modifier = Modifier.width(34.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    Slider(
                        value = v.coerceIn(0.3f, 3.0f),
                        onValueChange = { nv ->
                            if (r < editingRowWeights.size) {
                                editingRowWeights[r] = nv.coerceIn(0.3f, 3.0f)
                            }
                        },
                        onValueChangeFinished = {
                            onTemplateChange(templateState.copy(rowWeights = editingRowWeights.toList()))
                        },
                        valueRange = 0.3f..3.0f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format(Locale.US, "%.2f", v), modifier = Modifier.width(52.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("열 너비", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            for (c in 0 until templateState.cols.coerceAtLeast(1)) {
                val v = editingColWeights.getOrElse(c) { 1f }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("C${c + 1}", modifier = Modifier.width(34.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    Slider(
                        value = v.coerceIn(0.3f, 3.0f),
                        onValueChange = { nv ->
                            if (c < editingColWeights.size) {
                                editingColWeights[c] = nv.coerceIn(0.3f, 3.0f)
                            }
                        },
                        onValueChangeFinished = {
                            onTemplateChange(templateState.copy(colWeights = editingColWeights.toList()))
                        },
                        valueRange = 0.3f..3.0f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format(Locale.US, "%.2f", v), modifier = Modifier.width(52.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
            }
        }

        Text(
            "※ 값은 ‘비율’이며, 표 전체 크기 안에서 행/열 분배만 바뀜",
            style = DDZTypography.Caption,
            color = DDZColor.TextMuted
        )
    }
}
