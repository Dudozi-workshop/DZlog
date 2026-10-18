package com.dudoziworkshop.dzlog.ui.camera.controls

import com.dudoziworkshop.dzlog.feature.help.ui.*
import android.os.Build
import android.graphics.Rect
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.platform.LocalDensity
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
import com.dudoziworkshop.dzlog.feature.table.policy.*
import com.dudoziworkshop.dzlog.feature.table.policy.QuickEditSelectionMemory
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.ui.common.DDZCellTypeIcons
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.theme.*
import java.util.Date
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequest
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import com.dudoziworkshop.dzlog.feature.counter.camera.readCameraCounterSafely
import com.dudoziworkshop.dzlog.ui.camera.state.computeCameraDerivedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private class QuickValueDraft(initial: TableTemplateState) {
    var template by mutableStateOf(initial)
    var dirty = false
}

/** Table-first quick editing. Input finishes typing; Complete returns to the camera. */
@Composable
internal fun CameraQuickValueSheet(
    template: TableTemplateState,
    templateName: String,
    counterDigits: Int,
    counterSeed: Int?,
    counterRequest: CounterRequest?,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    phraseCursor: Int,
    now: Date,
    captureAspect: CaptureAspect,
    style: TableRenderStyle,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    saveMode: SaveMode,
    templates: List<SavedTableTemplate>,
    activeTemplateId: String?,
    selectionMemory: QuickEditSelectionMemory,
    onTemplateSelect: suspend (String, TableTemplateState) -> Boolean,
    onDismiss: () -> Unit,
    onCommit: (TableTemplateState) -> Unit,
) {
    val readOnlyGuide = LocalTutorialReadOnly.current
    val guidePage = if (readOnlyGuide) LocalTutorialLocation.current?.pageId else null
    val guideOn = LocalTutorialDemoProgress.current >= 0.5f
    val guideType = when (guidePage) {
        "quick-number" -> TableCellDataType.NUMBER
        "quick-date" -> TableCellDataType.DATE
        "quick-time" -> TableCellDataType.TIME
        "quick-phrase" -> TableCellDataType.ROTATING_TEXT
        "quick-counter" -> TableCellDataType.COUNTER
        else -> TableCellDataType.TEXT
    }
    val draft = remember { QuickValueDraft(template) }
    val previewTemplate = draft.template
    val cells = remember(previewTemplate.cells) { quickSelectableCells(previewTemplate) }
    val values = remember { cells.map { it.cellId to TextFieldValue(it.rawText, TextRange(it.rawText.length)) }.toMutableStateMap() }
    var selectedId by remember(activeTemplateId) {
        mutableStateOf(if (readOnlyGuide) cells.firstOrNull { it.dataType == guideType }?.cellId else selectionMemory.initialSelection(activeTemplateId, cells.map { it.cellId }))
    }
    LaunchedEffect(guidePage, guideOn) {
        if (!readOnlyGuide) return@LaunchedEffect
        val selected = template.cells.firstOrNull { it.dataType == guideType }
        draft.template = when {
            guidePage == "template" && guideOn -> template.copy(rows = 3, cols = 2,
                cells = template.cells.mapIndexed { i, c -> c.copy(rowIndex = i / 2, colIndex = i % 2) })
            guidePage == "quick-date" && selected != null -> template.copy(cells = template.cells.map {
                if (it.cellId == selected.cellId) it.copy(formatPattern = if (guideOn) "yyMMdd" else "yyyyMMdd") else it })
            guidePage == "quick-phrase" && selected != null -> template.copy(cells = template.cells.map {
                if (it.cellId == selected.cellId) it.copy(phraseSetId = if (guideOn) "guide-set-b" else "guide-set-a") else it })
            guidePage in listOf("quick-text", "quick-number") && selected != null -> {
                val text = if (guideOn) { if (guideType == TableCellDataType.NUMBER) "15" else "생육 조사" } else selected.rawText
                values[selected.cellId] = TextFieldValue(text)
                updateQuickCellValues(template, mapOf(selected.cellId to text))
            }
            else -> template
        }
    }
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requester = remember { FocusRequester() }
    val latestCommit = rememberUpdatedState(onCommit)
    val lifecycleOwner = LocalLifecycleOwner.current
    val resolver = remember { TableResolver() }
    val context = LocalContext.current
    val cameraHostView = LocalView.current
    val facade = remember(context, counterDigits) { CounterFacade(context, counterDigits) }
    var localCounterSeed by remember { mutableStateOf(counterSeed) }
    var localCounterRequest by remember { mutableStateOf(counterRequest) }
    val derived = remember(previewTemplate, now, counterDigits, phraseCursor, saveMode,
        includePathInCounterScope, includeFilenameInCounterScope, localCounterSeed, localCounterRequest) {
        computeCameraDerivedState(previewTemplate, now, counterDigits, phraseCursor,
            includePathInCounterScope, includeFilenameInCounterScope, saveMode,
            localCounterSeed, localCounterRequest, resolver)
    }
    val request = CounterRequestResolver.fromCamera(saveMode,
        derived.captureScopeState.counterScope.relativePathKey,
        derived.captureScopeState.counterScope.streamPrefix, derived.captureScopeState.scanPrefix,
        includePathInCounterScope, includeFilenameInCounterScope)
    // Read only the draft's counter stream. Camera state and image lookup stay untouched.
    LaunchedEffect(request) {
        if (readOnlyGuide) { localCounterSeed = 1; localCounterRequest = request; return@LaunchedEffect }
        if (localCounterRequest == request && localCounterSeed != null) return@LaunchedEffect
        delay(250)
        val result = readCameraCounterSafely { withContext(Dispatchers.IO) { facade.read(request) } }
        ensureActive()
        result.onSuccess { localCounterSeed = it.next; localCounterRequest = request }
    }
    val previewCounter = derived.displayCounter
    val resolvedCells = remember(previewTemplate, now, previewCounter, counterDigits, phraseCursor) {
        val phrases = PhraseResolver.resolveSelectedTextByCellId(previewTemplate.cells, previewTemplate.phraseSets, phraseCursor)
        val resolved = resolver.plan(previewTemplate.cells, now,
            TableResolver.Config(counterDigits, NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT),
            counterSeedOverride = previewCounter, selectedPhraseTextByCellId = phrases).resolvedCells
        if (previewCounter != null) resolved else resolved.map {
            if (it.type == TableCellDataType.COUNTER) it.copy(resolvedText = "", isEmpty = true) else it
        }
    }
    fun commit() {
        if (!readOnlyGuide && draft.dirty && !switching) { latestCommit.value(draft.template); draft.dirty = false }
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
        selectionMemory.recordSelection(activeTemplateId, id, cells.map { it.cellId })
        if (focused && cells.any {
                it.cellId == id &&
                    (it.dataType == TableCellDataType.TEXT || it.dataType == TableCellDataType.NUMBER)
            }
        ) requester.requestFocus()
    }
    GuideAwareDialog(onDismiss = { if (!switching) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false, dismissOnClickOutside = false)) {
        // Focus belongs to the dialog window; do not retain the camera window's manager.
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val context = LocalContext.current
        val view = LocalView.current
        val density = LocalDensity.current
        var hostHeight by remember(cameraHostView) {
            mutableIntStateOf(cameraHostView.rootView.height.coerceAtLeast(1))
        }
        var safeTop by remember { mutableIntStateOf(0) }
        var safeBottom by remember { mutableIntStateOf(0) }
        // Bound the content to the real host window. Dialog measurement and
        // Dialog-local Compose insets are not the viewport authority on API 36.
        DisposableEffect(view, cameraHostView) {
            fun updateViewport() {
                val root = cameraHostView.rootView
                val height = root.height.coerceAtLeast(1)
                val location = IntArray(2)
                root.getLocationOnScreen(location)
                val dialogLocation = IntArray(2)
                view.getLocationOnScreen(dialogLocation)
                val frame = Rect()
                view.getWindowVisibleDisplayFrame(frame)
                val hostInsets = ViewCompat.getRootWindowInsets(root)
                val dialogInsets = ViewCompat.getRootWindowInsets(view)
                val bars = hostInsets?.getInsets(WindowInsetsCompat.Type.systemBars())
                val ime = maxOf(
                    hostInsets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0,
                    dialogInsets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0,
                )
                val obscuredBottom = if (frame.height() > 0)
                    (location[1] + height - frame.bottom).coerceIn(0, height) else 0
                // The floating Dialog may begin below the host's top edge. Its
                // local origin must be subtracted before bounding its content.
                val dialogOffset = (dialogLocation[1] - location[1]).coerceAtLeast(0)
                hostHeight = (height - dialogOffset).coerceAtLeast(1)
                safeTop = (location[1] + (bars?.top ?: frame.top - location[1])
                    - dialogLocation[1]).coerceAtLeast(0)
                safeBottom = maxOf(bars?.bottom ?: 0, ime, obscuredBottom)
            }
            val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener { updateViewport() }
            val dialogObserver = view.viewTreeObserver
            val hostObserver = cameraHostView.viewTreeObserver
            dialogObserver.addOnGlobalLayoutListener(listener)
            hostObserver.addOnGlobalLayoutListener(listener)
            updateViewport()
            onDispose {
                if (dialogObserver.isAlive) dialogObserver.removeOnGlobalLayoutListener(listener)
                if (hostObserver.isAlive) hostObserver.removeOnGlobalLayoutListener(listener)
            }
        }
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                // A Compose Dialog has its own window; the Activity resize mode does not apply.
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
        fun selectCell(id: String) {
            val target = cells.firstOrNull { it.cellId == id } ?: return
            if (target.dataType != TableCellDataType.TEXT &&
                target.dataType != TableCellDataType.NUMBER
            ) inputDone()
            select(id)
        }
        fun dismiss() { inputDone(); commit(); onDismiss() }
        BoxWithConstraints(Modifier.heightIn(max = with(density) { hostHeight.toDp() })
            .fillMaxSize().background(DDZColor.PrimaryDark.copy(alpha = 0.72f))
            .padding(top = with(density) { safeTop.toDp() },
                bottom = with(density) { safeBottom.toDp() })
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            // Reserve a table viewport even with the keyboard and manual input expansion.
            val fixedChrome = DDZLayout.QuickInput.Touch * 2 + DDZLayout.Control.Compact * 2 +
                DDZSpacing.screenPadding * 2 + DDZSpacing.itemGap * 5 + DDZLayout.QuickInput.CellMarker * 2
            val inputBudget = (maxHeight - fixedChrome - DDZLayout.QuickInput.MinPreviewHeight)
                .coerceAtLeast(DDZLayout.QuickInput.Touch)
            val expandedHeight = minOf(DDZLayout.QuickInput.ExpandedHeight, inputBudget)
            // Keep the naming preview, table preview and editor in one IME-safe viewport.
            // The table owns the flexible space, so the keyboard shrinks only the table
            // instead of scrolling the previews off-screen or covering the editor.
            Column(Modifier.fillMaxWidth().height(maxHeight)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = DDZSpacing.screenPadding), verticalAlignment = Alignment.CenterVertically) {
                    Text("빠른 수정", style = DDZTypography.SettingLabel, color = DDZColor.Surface, modifier = Modifier.weight(1f))
                    TextButton(enabled = !switching, onClick = ::dismiss) { Text("완료", color = DDZColor.Surface) }
                }
                // Naming and table previews remain available in every keyboard state.
                CameraQuickNamingPreview(previewTemplate, resolvedCells, selectedId, counterDigits, previewCounter, now, saveMode,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DDZSpacing.screenPadding).tutorialTarget("camera.quick-naming"))
                CameraQuickTablePreview(previewTemplate, if (readOnlyGuide && guidePage == "template" && guideOn) "다른 표 양식 · 예시" else templateName, resolvedCells, style, captureAspect, tableWidthRatio, tableHeightRatio,
                    selectedId, cells.map { it.cellId }.toSet(), ::selectCell,
                    onLocked = { id -> Toast.makeText(context,
                        listOf("자동으로 표시되는 값이에요. 변경은 표 상세설정에서 가능해요.",
                            quickCellLinks(previewTemplate, id).description).filter { it.isNotBlank() }.joinToString("\n"),
                        Toast.LENGTH_SHORT).show() },
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
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .padding(DDZSpacing.screenPadding))
                Surface(Modifier.fillMaxWidth().tutorialTarget("camera.quick-editor"), color = DDZColor.Surface,
                    shape = RoundedCornerShape(topStart = DDZLayout.Radius.Medium, topEnd = DDZLayout.Radius.Medium)) {
                    Column(Modifier
                        .padding(horizontal = DDZSpacing.controlGap, vertical = DDZSpacing.itemGap)) {
                        val selected = cells.firstOrNull { it.cellId == selectedId }
                        if (selected == null) {
                            Text("변경할 수 있는 셀이 없습니다.", style = DDZTypography.Body, color = DDZColor.TextMuted)
                        } else {
                            val links = quickCellLinks(previewTemplate, selected.cellId)
                            val number = selected.dataType == TableCellDataType.NUMBER
                            val manual = selected.dataType == TableCellDataType.TEXT || number
                            val previous = adjacentQuickCellId(cells, selected.cellId, -1)
                            val next = adjacentQuickCellId(cells, selected.cellId, 1)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(when (selected.dataType) {
                                    TableCellDataType.NUMBER -> DDZCellTypeIcons.Number
                                    TableCellDataType.DATE -> DDZCellTypeIcons.Date
                                    TableCellDataType.TIME -> DDZCellTypeIcons.Time
                                    TableCellDataType.ROTATING_TEXT -> DDZCellTypeIcons.RotatingText
                                    else -> DDZCellTypeIcons.Text
                                }, null,
                                    tint = DDZColor.TextMuted, modifier = Modifier.size(DDZLayout.QuickInput.Icon))
                                Text(when (selected.dataType) {
                                    TableCellDataType.NUMBER -> "숫자"
                                    TableCellDataType.DATE -> "날짜"
                                    TableCellDataType.TIME -> "시간"
                                    TableCellDataType.ROTATING_TEXT -> "순환문구"
                                    else -> "텍스트"
                                }, style = DDZTypography.Secondary,
                                    modifier = Modifier.padding(horizontal = DDZSpacing.xs))
                                Text(quickCellPosition(selected), style = DDZTypography.Caption, color = DDZColor.TextMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (links.count > 0) IconButton(enabled = !switching, onClick = {
                                    Toast.makeText(context, links.description, Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(DDZSpacing.xs)) {
                                        if (links.fileName) Icon(Icons.Filled.Description, links.description,
                                            Modifier.size(DDZLayout.QuickInput.CellMarker), tint = DDZColor.SelectionEmphasis)
                                        if (links.path) Icon(Icons.Filled.Folder, if (links.fileName) null else links.description,
                                            Modifier.size(DDZLayout.QuickInput.CellMarker), tint = DDZColor.SelectionEmphasis)
                                    }
                                }
                                IconButton(enabled = previous != null && !switching, onClick = { previous?.let(::selectCell) }, modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
                                    Icon(Icons.Filled.ChevronLeft, "이전 셀", Modifier.size(DDZLayout.QuickInput.Icon))
                                }
                                IconButton(enabled = next != null && !switching, onClick = { next?.let(::selectCell) }, modifier = Modifier.size(DDZLayout.QuickInput.Touch)) {
                                    Icon(Icons.Filled.ChevronRight, "다음 셀", Modifier.size(DDZLayout.QuickInput.Icon))
                                }
                            }
                            if (!manual) {
                                CameraQuickAutoValueEditor(
                                    cell = selected,
                                    template = previewTemplate,
                                    now = now,
                                    enabled = !switching,
                                    onDateFormat = { pattern ->
                                        val updated = updateQuickDateFormat(draft.template, selected.cellId, pattern)
                                        if (updated != draft.template) { draft.template = updated; draft.dirty = true }
                                    },
                                    onPhraseSet = { id ->
                                        val updated = selectQuickPhraseSet(draft.template, selected.cellId, id)
                                        if (updated != draft.template) { draft.template = updated; draft.dirty = true }
                                    },
                                )
                            } else {
                            val value = values[selected.cellId] ?: TextFieldValue(selected.rawText)
                            fun change(nextValue: TextFieldValue) {
                                val accepted = if (number) decimalInputOrPrevious(nextValue.text, value.text) else nextValue.text
                                if (number && accepted != nextValue.text.replace('−', '-')) return
                                values[selected.cellId] = nextValue.copy(text = accepted)
                                if (accepted != value.text) {
                                    val updated = updateQuickCellValues(draft.template, mapOf(selected.cellId to accepted))
                                    draft.template = updated; draft.dirty = true
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                                Surface(Modifier.weight(1f), color = if (focused) DDZColor.SelectedSoft else DDZColor.Background,
                                    shape = RoundedCornerShape(DDZLayout.Radius.Small), border = BorderStroke(
                                        if (focused) DDZLayout.QuickInput.SelectionBorder else 1.dp,
                                        if (focused) DDZColor.SelectionEmphasis else DDZColor.Border)) {
                                    Box {
                                        BasicTextField(value = value, onValueChange = ::change, singleLine = number, readOnly = switching,
                                            modifier = Modifier.fillMaxWidth().height(if (expanded && !number) expandedHeight else DDZLayout.QuickInput.Touch)
                                                .focusRequester(requester).onFocusChanged { focused = it.isFocused }
                                                .padding(start = DDZSpacing.itemGap, end = DDZLayout.QuickInput.Touch, top = DDZSpacing.controlGap, bottom = DDZSpacing.itemGap),
                                            textStyle = DDZTypography.Body.copy(color = DDZColor.TextPrimary), cursorBrush = SolidColor(DDZColor.SelectionEmphasis),
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
                                TextButton(enabled = !switching, onClick = ::inputDone, modifier = Modifier.height(DDZLayout.QuickInput.Touch)) { Text("입력", color = DDZColor.SelectionEmphasis) }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}
