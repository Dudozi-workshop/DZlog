package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.table.policy.*
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZCellTypeIcons
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.theme.*

/** Immediate updates use the camera template pipeline; dismissal never rolls them back. */
@Composable
internal fun CameraQuickValueSheet(template: TableTemplateState, onDismiss: () -> Unit,
    onValueChange: (TableTemplateState) -> Unit, onCellFocus: (String?) -> Unit,
) {
    val cells = quickEditableCells(template)
    val values = remember { cells.map { it.cellId to it.rawText }.toMutableStateMap() }
    val focus = LocalFocusManager.current
    val requesters = remember(cells.map { it.cellId }) { cells.map { FocusRequester() } }
    DisposableEffect(Unit) { onDispose { onCellFocus(null) } }
    fun dismiss() { focus.clearFocus(); onCellFocus(null); onDismiss() }
    DDZBottomSheet(onDismiss = ::dismiss) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("빠른 값 변경", style = DDZTypography.SectionTitle, modifier = Modifier.weight(1f))
            TextButton(onClick = ::dismiss) { Text("완료") }
        }
        val listHeight = (DDZLayout.QuickInput.RowHeight * cells.size)
            .coerceIn(DDZLayout.QuickInput.MinListHeight, DDZLayout.QuickInput.MaxListHeight)
        Column(Modifier.fillMaxWidth().height(listHeight).verticalScroll(rememberScrollState())) {
            if (cells.isEmpty()) Text("변경할 값이 없습니다.", color = DDZColor.TextMuted)
            cells.forEachIndexed { index, cell -> key(cell.cellId) {
                QuickValueRow(cell, template, values[cell.cellId] ?: cell.rawText, requesters[index],
                    onFocus = { onCellFocus(if (it) cell.cellId else null) },
                    onNext = { if (index < cells.lastIndex) requesters[index + 1].requestFocus() else focus.clearFocus() },
                    onChange = { text -> values[cell.cellId] = text; onValueChange(updateQuickCellValues(template, values.toMap())) },
                )
            } }
        }
    }
}

@Composable
private fun QuickValueRow(cell: TableCellState, template: TableTemplateState, initialText: String,
    requester: FocusRequester, onFocus: (Boolean) -> Unit, onNext: () -> Unit, onChange: (String) -> Unit,
) {
    var value by remember(cell.cellId) { mutableStateOf(TextFieldValue(initialText)) }
    var focused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(0) }
    var showLocation by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val expandedFocus = remember { FocusRequester() }
    val number = cell.dataType == TableCellDataType.NUMBER
    fun change(next: TextFieldValue) {
        val accepted = if (number) decimalInputOrPrevious(next.text, value.text) else next.text
        if (number && accepted == value.text && next.text != value.text && next.text.replace('−', '-') != accepted) return
        value = next.copy(text = accepted); onChange(accepted)
    }
    fun shrink() { expanded = false; requester.requestFocus() }
    Box(Modifier.fillMaxWidth().height(DDZLayout.QuickInput.RowHeight).onSizeChanged { widthPx = it.width }) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (!focused && !expanded) Icon(if (number) DDZCellTypeIcons.Number else DDZCellTypeIcons.Text,
                if (number) "숫자" else "텍스트", tint = DDZColor.TextMuted, modifier = Modifier.size(DDZLayout.QuickInput.Icon))
            Surface(modifier = Modifier.weight(1f).padding(horizontal = DDZSpacing.itemGap),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(DDZLayout.Radius.Small), color = DDZColor.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (focused) DDZColor.SageBorder else DDZColor.Border),
            ) {
                BasicTextField(value = value, onValueChange = ::change, singleLine = true, readOnly = expanded,
                    modifier = Modifier.fillMaxWidth().height(DDZLayout.QuickInput.Touch).focusRequester(requester)
                        .onFocusChanged { focused = it.isFocused; if (!expanded) onFocus(it.isFocused) }
                        .padding(horizontal = DDZSpacing.controlGap, vertical = DDZSpacing.controlGap),
                    textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary), cursorBrush = SolidColor(DDZColor.SageDarkStrong),
                    keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { onNext() }),
                    decorationBox = { field ->
                        Box {
                            if (value.text.isEmpty()) Text("값 입력", color = DDZColor.TextMuted, style = DDZTypography.Body)
                            field()
                            if (!focused && value.text.contains('\n')) Text(value.text.lineSequence().first() + " …", modifier = Modifier.fillMaxWidth().background(DDZColor.Surface), style = DDZTypography.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                )
            }
            if (focused || expanded) Box(Modifier.size(DDZLayout.QuickInput.Touch), contentAlignment = Alignment.Center) {
                IconButton(onClick = {
                    if (number) { val text = if (value.text.startsWith("-")) value.text.drop(1) else "-${value.text}"; change(TextFieldValue(text, TextRange(text.length))); requester.requestFocus() }
                    else expanded = true
                }) {
                    if (number) Text("±", style = DDZTypography.SettingLabel)
                    else Icon(Icons.Filled.OpenInFull, "입력창 확대", modifier = Modifier.size(DDZLayout.QuickInput.Icon))
                }
            }
            if (!focused && !expanded) Box {
                IconButton(onClick = { showLocation = !showLocation }, modifier = Modifier.size(DDZLayout.QuickInput.Touch).semantics { contentDescription = "셀 위치: ${quickCellPosition(cell)}" }) {
                    QuickCellMap(template, cell.cellId, Modifier.size(DDZLayout.QuickInput.MapWidth, DDZLayout.QuickInput.MapHeight))
                }
                if (showLocation) Popup(alignment = Alignment.TopEnd, onDismissRequest = { showLocation = false }, properties = PopupProperties(focusable = true)) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(DDZLayout.Radius.Small), color = DDZColor.SurfaceSoft, shadowElevation = 2.dp) {
                        Text(quickCellPosition(cell), modifier = Modifier.padding(DDZSpacing.itemGap), style = DDZTypography.Caption)
                    }
                }
            }
        }
        if (expanded) {
            Popup(alignment = Alignment.BottomCenter, onDismissRequest = ::shrink, properties = PopupProperties(focusable = true)) {
                Surface(modifier = Modifier.width(with(density) { widthPx.toDp() }).height(DDZLayout.QuickInput.ExpandedHeight),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(DDZLayout.Radius.Medium), color = DDZColor.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DDZColor.SageBorder), shadowElevation = 4.dp,
                ) {
                    Column(Modifier.padding(DDZSpacing.itemGap)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("텍스트 · ${quickCellPosition(cell)}", style = DDZTypography.Caption, modifier = Modifier.weight(1f))
                            IconButton(onClick = ::shrink) { Icon(Icons.Filled.CloseFullscreen, "입력창 축소", Modifier.size(DDZLayout.QuickInput.Icon)) }
                        }
                        BasicTextField(value = value, onValueChange = ::change,
                            modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(expandedFocus),
                            textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary), cursorBrush = SolidColor(DDZColor.SageDarkStrong),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                        )
                    }
                }
            }
            LaunchedEffect(Unit) { expandedFocus.requestFocus(); onFocus(true) }
        }
    }
}

@Composable
private fun QuickCellMap(template: TableTemplateState, selectedId: String, modifier: Modifier) {
    Canvas(modifier) {
        fun edges(count: Int, weights: List<Float>?, length: Float): List<Float> {
            val safe = List(count) { weights?.getOrNull(it)?.takeIf { w -> w.isFinite() && w > 0f } ?: 1f }
            val total = safe.sum(); var cursor = 0f
            return listOf(0f) + safe.map { cursor += it / total * length; cursor }
        }
        val xs = edges(template.cols, template.colWeights, size.width)
        val ys = edges(template.rows, template.rowWeights, size.height)
        quickRootCells(template).forEach { cell ->
            if (cell.rowIndex in 0 until template.rows && cell.colIndex in 0 until template.cols) {
                val offset = Offset(xs[cell.colIndex], ys[cell.rowIndex])
                val rectSize = Size(xs[(cell.colIndex + cell.colSpan).coerceAtMost(template.cols)] - offset.x, ys[(cell.rowIndex + cell.rowSpan).coerceAtMost(template.rows)] - offset.y)
                drawRect(if (cell.cellId == selectedId) DDZColor.SageDarkStrong else DDZColor.SurfaceSoft, offset, rectSize)
                drawRect(DDZColor.Border, offset, rectSize, style = Stroke(0.6.dp.toPx()))
            }
        }
    }
}
