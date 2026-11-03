package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.*
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlin.math.roundToInt

/** Explicit user confirmation supplies placement for new and legacy iOS documents. */
@Composable
internal fun IosCaptureSettingsScreen(
    document: EditorDocument,
    onBack: () -> Unit,
    onApply: (EditorDocument) -> Unit,
) {
    var mode by remember(document) { mutableStateOf(document.saveMode) }
    // Draft suggestions only: Cancel never writes these values to the document.
    var placement by remember(document) { mutableStateOf(document.capturePlacement ?:
        EditorCapturePlacement(EditorCaptureAnchor.BOTTOM_RIGHT, 50, 50, 60, 20)) }
    var number by remember(document) { mutableStateOf((document.nextCounter ?: 1).toString()) }
    val next = number.toIntOrNull()?.takeIf { it > 0 && it < Int.MAX_VALUE }
    val merged = document.template.templateState.cells.any { it.rowSpan != 1 || it.colSpan != 1 }
    Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding()
        .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("촬영 설정")
        Text(document.template.name)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("취소") }
            Button(enabled = next != null && (mode == SaveMode.ORIGINAL_ONLY || !merged), onClick = {
                onApply(document.copy(saveMode = mode, nextCounter = next,
                    usesAutoNext = if (next != document.nextCounter) false else document.usesAutoNext,
                    capturePlacement = if (mode == SaveMode.ORIGINAL_ONLY) document.capturePlacement else placement))
            }) { Text("설정 저장") }
        }
        Text("저장 모드")
        for ((value, label) in listOf(SaveMode.ORIGINAL_ONLY to "원본만",
            SaveMode.WATERMARK_ONLY to "결과만", SaveMode.BOTH to "원본과 결과")) {
            Row {
                RadioButton(selected = mode == value, onClick = { mode = value })
                TextButton(onClick = { mode = value }) { Text(label) }
            }
        }
        OutlinedTextField(value = number, onValueChange = { if (it.all(Char::isDigit)) number = it },
            label = { Text("다음 촬영 번호") }, singleLine = true, isError = next == null)
        if (mode != SaveMode.ORIGINAL_ONLY) {
            if (merged) Text("병합된 표는 아직 결과 사진 저장을 지원하지 않습니다. 원본만 저장할 수 있습니다.")
            Text("표 위치")
            for ((anchor, label) in listOf(EditorCaptureAnchor.TOP_LEFT to "왼쪽 위",
                EditorCaptureAnchor.TOP_RIGHT to "오른쪽 위", EditorCaptureAnchor.BOTTOM_LEFT to "왼쪽 아래",
                EditorCaptureAnchor.BOTTOM_RIGHT to "오른쪽 아래", EditorCaptureAnchor.CUSTOM to "사용자 지정")) {
                Row {
                    RadioButton(selected = placement.anchor == anchor, onClick = {
                        placement = placement.copy(anchor = anchor, rotationCwDeg = 0)
                    })
                    TextButton(onClick = { placement = placement.copy(anchor = anchor, rotationCwDeg = 0) }) { Text(label) }
                }
            }
            CaptureRatio("표 너비", placement.widthRatio, 10f..100f) { placement = placement.copy(widthRatio = it) }
            CaptureRatio("표 높이", placement.heightRatio, 10f..100f) { placement = placement.copy(heightRatio = it) }
            Text("표 너비와 높이는 사진 너비를 기준으로 적용됩니다.")
            if (placement.anchor == EditorCaptureAnchor.CUSTOM) {
                CaptureRatio("가로 위치", placement.offsetXPercent, 0f..100f) { placement = placement.copy(offsetXPercent = it) }
                CaptureRatio("세로 위치", placement.offsetYPercent, 0f..100f) { placement = placement.copy(offsetYPercent = it) }
                Text("위치는 회전된 표를 제외한 여유 공간의 비율입니다.")
                Row {
                    Switch(checked = placement.rotationCwDeg == 90,
                        onCheckedChange = { placement = placement.copy(rotationCwDeg = if (it) 90 else 0) })
                    Text("90도 회전")
                }
            }
        }
    }
}

@Composable
private fun CaptureRatio(label: String, value: Int, range: ClosedFloatingPointRange<Float>, onChange: (Int) -> Unit) {
    Text("$label: $value%")
    Slider(value = value.toFloat(), onValueChange = { onChange(it.roundToInt()) }, valueRange = range)
}
