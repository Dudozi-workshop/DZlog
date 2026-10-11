package com.dudoziworkshop.dzlog.ui.camera.controls

import android.os.Build
import android.widget.Toast
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.phrase.PhraseResolver
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.feature.table.policy.*
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.ui.common.DDZCellTypeIcons
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.theme.*
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private class QuickValueDraft(var template: TableTemplateState, var dirty: Boolean = false)

/** Table-first quick editing. Input finishes typing; Complete returns to the camera. */
@Composable
internal fun CameraQuickValueSheet(
    template: TableTemplateState,
    templateName: String,
    counterDigits: Int,
    counterSeed: Int?,
    phraseCursor: Int,
    now: Date,
    captureAspect: CaptureAspect,
    style: TableRenderStyle,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    saveMode: SaveMode,
    templates: List<SavedTableTemplate>,
    activeTemplateId: String?,
    onTemplateSelect: suspend (String, TableTemplateState) -> Boolean,
    onDismiss: () -> Unit,
    onValueChange: (TableTemplateState) -> Unit,
    onCommit: (TableTemplateState) -> Unit,
) {
    val cells = remember(template.cells) { quickEditableCells(template) }
    val draft = remember { QuickValueDraft(template) }
    val values = remember { cells.map { it.cellId to TextFieldValue(it.rawText, TextRange(it.rawText.length)) }.toMutableStateMap() }
    var selectedId by remember { mutableStateOf(cells.firstOrNull()?.cellId) }
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requester = remember { FocusRequester() }
    val latestCommit = rememberUpdatedState(onCommit)
    val lifecycleOwner = LocalLifecycleOwner.current
    val resolver = remember { TableResolver() }
    val resolvedCells = remember(template, now, counterSeed, counterDigits, phraseCursor) {
        val phrases = PhraseResolver.resolveSelectedTextByCellId(template.cells, template.phraseSets, phraseCursor)
        val resolved = resolver.plan(template.cells, now,
            TableResolver.Config(counterDigits, NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT),
            counterSeedOverride = counterSeed, selectedPhraseTextByCellId = phrases).resolvedCells
        if (counterSeed != null) resolved else resolved.map {
            if (it.type == TableCellDataType.COUNTER) it.copy(resolvedText = "", isEmpty = true) else it
        }
    }
    val watermarkCells = remember(resolvedCells) { WatermarkBuilder.buildTableCells(resolvedCells) }
    fun commit() {
        if (draft.dirty && !switching) { latestCommit.value(draft.template); draft.dirty = false }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) commit() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); commit() }
    }
    fun select(id: String) {
        if (cells.none { it.cellId == id }) return
        selectedId?.let { oldId -> values[oldId]?.let { values[oldId] = it.copy(composition = null) } }
        values[id]?.let { values[id] = it.copy(composition = null) }
        selectedId = id
        if (focused) requester.requestFocus()
    }
    Dialog(onDismissRequest = { if (!switching) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false, dismissOnClickOutside = false)) {
        // Focus belongs to the dialog window; do not retain the camera window's manager.
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val context = LocalContext.current
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                WindowInsetsControllerCompat(window, view).apply {
                    isAppearanceLightNavigationBars = true
                    isAppearanceLightStatusBars = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
            }
            onDispose { }
        }
        fun inputDone() { focus.clearFocus(); keyboard?.hide() }
        fun dismiss() { inputDone(); commit(); onDismiss() }
        BoxWithConstraints(Modifier.fillMaxSize().background(DDZColor.PrimaryDark.copy(alpha = 0.72f))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)).imePadding()) {
            val expandedHeight = (maxHeight * 0.3f).coerceIn(DDZLayout.QuickInput.Touch, DDZLayout.QuickInput.ExpandedHeight)
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = DDZSpacing.screenPadding), verticalAlignment = Alignment.CenterVertically) {
                    Text("빠른 수정", style = DDZTypography.SettingLabel, color = DDZColor.Surface, modifier = Modifier.weight(1f))
                    TextButton(enabled = !switching, onClick = ::dismiss) { Text("완료", color = DDZColor.Surface) }
                }
                CameraQuickNamingPreview(template, resolvedCells, selectedId, counterDigits, counterSeed, now, saveMode,
                    compact = focused || expanded,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DDZSpacing.screenPadding))
                CameraQuickTablePreview(template, templateName, watermarkCells, style, captureAspect, tableWidthRatio, tableHeightRatio,
                    selectedId, cells.map { it.cellId }.toSet(), ::select,
                    onLocked = { Toast.makeText(context,
                        "자동으로 표시되는 값이에요. 변경은 표 상세설정에서 가능해요.", Toast.LENGTH_SHORT).show() },
                    templates = templates, activeTemplateId = activeTemplateId, switching = switching,
                    onTemplateSelect = { id ->
                        inputDone()
                        switching = true
                        scope.launch {
                            try {
                                if (onTemplateSelect(id, draft.template)) {
                                    // Switching persists this session before activating the next one.
                                    // Its disposal must never write an old draft into the new active template.
                                    draft.dirty = false
                                } else Toast.makeText(context, "템플릿 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } finally { switching = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(DDZSpacing.screenPadding))
                Surface(Modifier.fillMaxWidth(), color = DDZColor.Surface,
                    shape = RoundedCornerShape(topStart = DDZLayout.Radius.Medium, topEnd = DDZLayout.Radius.Medium)) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = DDZSpacing.controlGap, vertical = DDZSpacing.itemGap)) {
                        val selected = cells.firstOrNull { it.cellId == selectedId }
                        if (selected == null) {
                            Text("변경할 텍스트·숫자 셀이 없습니다.", style = DDZTypography.Body, color = DDZColor.TextMuted)
                        } else {
                            val number = selected.dataType == TableCellDataType.NUMBER
                            val previous = adjacentQuickCellId(cells, selected.cellId, -1)
                            val next = adjacentQuickCellId(cells, selected.cellId, 1)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (number) DDZCellTypeIcons.Number else DDZCellTypeIcons.Text, null,
                                    tint = DDZColor.TextMuted, modifier = Modifier.size(DDZLayout.QuickInput.Icon))
                                Text(if (number) "숫자" else "텍스트", style = DDZTypography.Secondary,
                                    modifier = Modifier.padding(horizontal = DDZSpacing.xs))
                                Text(quickCellPosition(selected), style = DDZTypography.Caption, color = DDZColor.TextMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                IconButton(enabled = previous != null && !switching, onClick = { previous?.let(::select) }, modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
                                    Icon(Icons.Filled.ChevronLeft, "이전 셀", Modifier.size(DDZLayout.QuickInput.Icon))
                                }
                                IconButton(enabled = next != null && !switching, onClick = { next?.let(::select) }, modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
                                    Icon(Icons.Filled.ChevronRight, "다음 셀", Modifier.size(DDZLayout.QuickInput.Icon))
                                }
                            }
                            val value = values[selected.cellId] ?: TextFieldValue(selected.rawText)
                            fun change(nextValue: TextFieldValue) {
                                val accepted = if (number) decimalInputOrPrevious(nextValue.text, value.text) else nextValue.text
                                if (number && accepted != nextValue.text.replace('−', '-')) return
                                values[selected.cellId] = nextValue.copy(text = accepted)
                                if (accepted != value.text) {
                                    val updated = updateQuickCellValues(draft.template, mapOf(selected.cellId to accepted))
                                    draft.template = updated; draft.dirty = true; onValueChange(updated)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                                Surface(Modifier.weight(1f), color = DDZColor.Background,
                                    shape = RoundedCornerShape(DDZLayout.Radius.Small), border = BorderStroke(1.dp, if (focused) DDZColor.SageBorder else DDZColor.Border)) {
                                    Box {
                                        BasicTextField(value = value, onValueChange = ::change, singleLine = number, readOnly = switching,
                                            modifier = Modifier.fillMaxWidth().height(if (expanded && !number) expandedHeight else DDZLayout.QuickInput.Touch)
                                                .focusRequester(requester).onFocusChanged { focused = it.isFocused }
                                                .padding(start = DDZSpacing.itemGap, end = DDZLayout.QuickInput.Touch, top = DDZSpacing.controlGap, bottom = DDZSpacing.itemGap),
                                            textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary), cursorBrush = SolidColor(DDZColor.SageDarkStrong),
                                            keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text,
                                                imeAction = if (number) ImeAction.Done else ImeAction.Default),
                                            keyboardActions = KeyboardActions(onDone = { inputDone() }),
                                            decorationBox = { field -> Box {
                                                if (value.text.isEmpty()) Text("값 입력", style = DDZTypography.Body, color = DDZColor.TextMuted)
                                                field()
                                            } })
                                        IconButton(enabled = !switching, onClick = {
                                            if (number) {
                                                val text = if (value.text.startsWith("-")) value.text.drop(1) else "-${value.text}"
                                                change(TextFieldValue(text, TextRange(text.length)))
                                            } else expanded = !expanded
                                        }, modifier = Modifier.align(Alignment.BottomEnd).size(DDZLayout.QuickInput.Touch)) {
                                            if (number) Text("±", style = DDZTypography.SettingLabel)
                                            else Icon(if (expanded) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                                                if (expanded) "입력창 축소" else "입력창 확대", Modifier.size(DDZLayout.QuickInput.Icon))
                                        }
                                    }
                                }
                                TextButton(enabled = !switching, onClick = ::inputDone, modifier = Modifier.height(DDZLayout.QuickInput.Touch)) { Text("입력", color = DDZColor.SageDarkStrong) }
                            }
                        }
                    }
                }
            }
        }
    }
}
