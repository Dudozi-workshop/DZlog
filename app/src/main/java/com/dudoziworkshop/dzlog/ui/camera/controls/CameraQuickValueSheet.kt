package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.table.policy.*
import com.dudoziworkshop.dzlog.ui.common.DDZCellTypeIcons
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.theme.*

private class QuickValueDraft(var template: TableTemplateState, var dirty: Boolean = false)

/** Inline panel reserves camera space; only the selected cell owns an input connection. */
@Composable
internal fun CameraQuickValueSheet(
    template: TableTemplateState,
    maxPanelHeight: Dp,
    onDismiss: () -> Unit,
    onValueChange: (TableTemplateState) -> Unit,
    onCommit: (TableTemplateState) -> Unit,
    onCellFocus: (String?) -> Unit,
) {
    val cells = remember(template.cells) { quickEditableCells(template) }
    val roots = remember(template.cells) { quickRootCells(template) }
    val draft = remember { QuickValueDraft(template) }
    val latestCommit = rememberUpdatedState(onCommit)
    val latestFocus = rememberUpdatedState(onCellFocus)
    var selectedId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var locationId by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun commit() {
        if (draft.dirty) {
            latestCommit.value(draft.template)
            draft.dirty = false
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) commit()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            commit()
            latestFocus.value(null)
        }
    }
    fun list() {
        focus.clearFocus(); keyboard?.hide()
        expanded = false; selectedId = null; onCellFocus(null)
    }
    fun dismiss() { focus.clearFocus(); keyboard?.hide(); commit(); onDismiss() }
    BackHandler {
        when {
            expanded -> expanded = false
            selectedId != null -> list()
            else -> dismiss()
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = DDZLayout.Radius.Medium, topEnd = DDZLayout.Radius.Medium),
        color = DDZColor.Surface, border = BorderStroke(1.dp, DDZColor.Border),
    ) {
        Column(Modifier.padding(horizontal = DDZSpacing.controlGap, vertical = DDZSpacing.xs)) {
            val selected = cells.firstOrNull { it.cellId == selectedId }
            if (selected == null) {
                Row(Modifier.fillMaxWidth().height(DDZLayout.QuickInput.Touch), verticalAlignment = Alignment.CenterVertically) {
                    Text("빠른 값 변경", style = DDZTypography.SettingLabel, modifier = Modifier.weight(1f))
                    TextButton(onClick = ::dismiss) { Text("완료") }
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = (maxPanelHeight - DDZLayout.QuickInput.Touch - DDZSpacing.itemGap).coerceAtLeast(0.dp))) {
                    if (cells.isEmpty()) item { Text("변경할 값이 없습니다.", color = DDZColor.TextMuted) }
                    items(cells, key = { it.cellId }) { cell ->
                        Row(
                            Modifier.fillMaxWidth().height(DDZLayout.QuickInput.RowHeight)
                                .clickable { selectedId = cell.cellId; onCellFocus(cell.cellId) }
                                .semantics { contentDescription = "${quickCellPosition(cell)}, ${if (cell.dataType == TableCellDataType.NUMBER) "숫자" else "텍스트"}, ${cell.rawText}, 수정" },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (cell.dataType == TableCellDataType.NUMBER) DDZCellTypeIcons.Number else DDZCellTypeIcons.Text,
                                null, tint = DDZColor.TextMuted, modifier = Modifier.size(DDZLayout.QuickInput.Icon))
                            Text(cell.rawText.ifEmpty { "값 입력" }.replace('\n', ' '),
                                modifier = Modifier.weight(1f).padding(horizontal = DDZSpacing.itemGap),
                                style = DDZTypography.Body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (cell.rawText.isEmpty()) DDZColor.TextMuted else DDZColor.TextPrimary)
                            if (locationId == cell.cellId) Text(quickCellPosition(cell), style = DDZTypography.Caption)
                            IconButton(onClick = { locationId = if (locationId == cell.cellId) null else cell.cellId },
                                modifier = Modifier.semantics { contentDescription = "셀 위치: ${quickCellPosition(cell)}" }) {
                                QuickCellMap(template, roots, cell.cellId,
                                    Modifier.size(DDZLayout.QuickInput.MapWidth, DDZLayout.QuickInput.MapHeight))
                            }
                        }
                    }
                }
            } else {
                key(selected.cellId) {
                    SelectedValueInput(selected, expanded,
                        expandedHeight = (maxPanelHeight - DDZSpacing.itemGap).coerceAtMost(DDZLayout.QuickInput.ExpandedHeight),
                        onBack = ::list, onDone = ::dismiss, onExpand = { expanded = !expanded },
                        onChange = { text ->
                            val updated = updateQuickCellValues(draft.template, mapOf(selected.cellId to text))
                            draft.template = updated; draft.dirty = true
                            onValueChange(updated)
                        })
                }
            }
        }
    }
}

@Composable
private fun SelectedValueInput(cell: TableCellState, expanded: Boolean, expandedHeight: Dp,
    onBack: () -> Unit, onDone: () -> Unit, onExpand: () -> Unit, onChange: (String) -> Unit,
) {
    var value by remember(cell.cellId) { mutableStateOf(TextFieldValue(cell.rawText, TextRange(cell.rawText.length))) }
    val requester = remember { FocusRequester() }
    val number = cell.dataType == TableCellDataType.NUMBER
    fun change(next: TextFieldValue) {
        val accepted = if (number) decimalInputOrPrevious(next.text, value.text) else next.text
        if (number && accepted != next.text.replace('−', '-')) return
        val changed = accepted != value.text
        value = next.copy(text = accepted)
        if (changed) onChange(accepted)
    }
    LaunchedEffect(Unit) { requester.requestFocus() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        IconButton(onClick = onBack, modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
            Icon(Icons.Filled.ArrowBack, "셀 목록", Modifier.size(DDZLayout.QuickInput.Icon))
        }
        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(DDZLayout.Radius.Small),
            color = DDZColor.Surface, border = BorderStroke(1.dp, DDZColor.SageBorder)) {
            BasicTextField(value = value, onValueChange = ::change, singleLine = number,
                modifier = Modifier.fillMaxWidth().height(if (expanded) expandedHeight else DDZLayout.QuickInput.Touch)
                    .focusRequester(requester).padding(horizontal = DDZSpacing.itemGap, vertical = DDZSpacing.controlGap)
                    .semantics { contentDescription = "${quickCellPosition(cell)} 값" },
                textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary), cursorBrush = SolidColor(DDZColor.SageDarkStrong),
                keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text,
                    imeAction = if (number) ImeAction.Done else ImeAction.Default),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                decorationBox = { field -> Box {
                    if (value.text.isEmpty()) Text("값 입력", color = DDZColor.TextMuted, style = DDZTypography.Body)
                    field()
                } },
            )
        }
        IconButton(onClick = {
            if (number) {
                val text = if (value.text.startsWith("-")) value.text.drop(1) else "-${value.text}"
                change(TextFieldValue(text, TextRange(text.length)))
            } else onExpand()
        }, modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
            if (number) Text("±", style = DDZTypography.SettingLabel)
            else Icon(if (expanded) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                if (expanded) "입력창 축소" else "입력창 확대", Modifier.size(DDZLayout.QuickInput.Icon))
        }
        IconButton(onClick = onDone, modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
            Icon(Icons.Filled.Check, "완료", Modifier.size(DDZLayout.QuickInput.Icon))
        }
    }
}

@Composable
private fun QuickCellMap(template: TableTemplateState, roots: List<TableCellState>, selectedId: String, modifier: Modifier) {
    fun edges(count: Int, weights: List<Float>?): List<Float> {
        val safe = List(count) { weights?.getOrNull(it)?.takeIf { w -> w.isFinite() && w > 0f } ?: 1f }
        val total = safe.sum(); var cursor = 0f
        return listOf(0f) + safe.map { cursor += it / total; cursor }
    }
    val xs = remember(template.cols, template.colWeights) { edges(template.cols, template.colWeights) }
    val ys = remember(template.rows, template.rowWeights) { edges(template.rows, template.rowWeights) }
    Canvas(modifier) {
        roots.forEach { cell ->
            if (cell.rowIndex in 0 until template.rows && cell.colIndex in 0 until template.cols) {
                val offset = Offset(xs[cell.colIndex] * size.width, ys[cell.rowIndex] * size.height)
                val rectSize = Size(xs[(cell.colIndex + cell.colSpan).coerceAtMost(template.cols)] * size.width - offset.x,
                    ys[(cell.rowIndex + cell.rowSpan).coerceAtMost(template.rows)] * size.height - offset.y)
                drawRect(if (cell.cellId == selectedId) DDZColor.SageDarkStrong else DDZColor.SurfaceSoft, offset, rectSize)
                drawRect(DDZColor.Border, offset, rectSize, style = Stroke(0.6.dp.toPx()))
            }
        }
    }
}
