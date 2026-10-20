package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.ui.table.mock.TableEditorV2Content
import com.dudoziworkshop.dzlog.ui.table.mock.formatMockCounter
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.ui.common.DDZConfirmDialog
import com.dudoziworkshop.dzlog.ui.home.*
import com.dudoziworkshop.dzlog.ui.table.template.TableTemplateListContent
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.Foundation.*

/** Existing shared home/list/editor flow. Editor documents persist in the iOS sandbox. */
@Composable
internal fun HomeNavigationPoc(template: SavedTableTemplate, editorKey: String = IOS_EDITOR_KEY, resetTestFixture: Boolean = false) {
    val store = remember(editorKey) { IosEditorStore(editorKey) }
    val initial = remember(store) {
        if (resetTestFixture) store.resetTestFixture()
        runCatching { store.load() }
    }
    var document by remember { mutableStateOf(initial.getOrNull() ?: EditorDocument(template)) }
    val loadFailed = initial.isFailure
    var notice by remember { mutableStateOf(if (loadFailed) "기존 편집 문서를 읽지 못했습니다. 덮어쓰지 않도록 편집을 잠갔습니다." else null) }
    val editorPlatform = remember { IosTableEditorPlatform { notice = it } }
    var showEditor by remember { mutableStateOf(false) }
    val currentTemplate = document.template
    var showList by remember { mutableStateOf(false) }
    var showSaveInfo by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(initial.getOrNull() != null) }
    val leaf = rememberHomeLeafPainter()
    val savedPreview = remember(document) {
        buildPreview(PreviewInput(currentTemplate.templateState, tableEditorNow(), document.counterPadding,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, document.includePathInScope, document.includeFilenameInScope,
            document.saveMode, document.nextCounter, 0))
    }
    if (showEditor) {
        key(currentTemplate.id) {
            TableEditorV2Content(
                platform = editorPlatform, templateState = currentTemplate.templateState,
                styleState = currentTemplate.styleState, includePathInCounterScope = document.includePathInScope,
                includeFilenameInCounterScope = document.includeFilenameInScope,
                saveMode = document.saveMode, counterPadding = document.counterPadding,
                templateId = currentTemplate.id, templateName = currentTemplate.name,
                templates = listOf(currentTemplate),
                onBack = { showEditor = false },
                onSave = { table, style, pathScope, filenameScope, mode, padding, next, autoNext ->
                    val updated = EditorDocument(
                        currentTemplate.copy(templateState = table, styleState = style,
                            modifiedAt = (NSDate().timeIntervalSince1970 * 1000).toLong()),
                        pathScope, filenameScope, mode, padding, next, autoNext,
                    )
                    if (!loadFailed && store.save(updated)) {
                        document = updated
                        selected = true
                        true
                    } else false
                },
                initialNextCounter = document.nextCounter, initialUsesAutoNext = document.usesAutoNext,
            )
        }
    } else if (showList) {
        TableTemplateListContent(
            templates = listOf(currentTemplate), activeTemplateId = if (selected) currentTemplate.id else null,
            onBack = { showList = false }, onSelectTemplate = { selected = true; showList = false },
            allowManagement = false,
            onOpenTemplate = {}, onCreateTemplate = {}, onRenameTemplate = { _, _ -> },
            onDuplicateTemplate = {}, onDeleteTemplate = {},
            formatDate = { _, used -> if (used) "사용 기록 없음" else "수정일 없음" },
        )
    } else {
        HomeContent(
            templateName = if (selected) currentTemplate.name else "기본 촬영",
            labels = HomeLabels("설정", "현재 템플릿",
                "다음 촬영 번호 " + if (selected) formatMockCounter(document.nextCounter ?: 1, document.counterPadding) else "0001",
                "촬영 시작", "표 편집", "저장 정보"),
            onManageTemplates = { showList = true }, onOpenSettings = {}, onStartCamera = {},
            onOpenTableEditor = { showEditor = true }, onOpenSaveInfo = { showSaveInfo = true },
            modifier = Modifier.fillMaxSize().background(DDZColor.Background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
            captureEnabled = false, settingsEnabled = false, editingEnabled = !loadFailed, saveInfoEnabled = true,
            background = { HomeAmbientContent(painter = leaf) },
            recentCapture = {
                HomeRecentCaptureContent(
                    hasImage = false, displayName = "", timeText = "", recentLabel = "최근 촬영",
                    emptyTitle = "아직 촬영한 사진이 없어요.", emptyHint = "첫 기록을 남겨보세요.",
                    onOpenAlbum = {}, onOpenImage = {}, albumEnabled = false,
                )
            },
        )
    }
    notice?.let { message ->
        DDZConfirmDialog(title = "안내", message = message, confirmText = "확인", dismissText = "닫기",
            onConfirm = { notice = null }, onDismiss = { notice = null })
    }
    if (showSaveInfo) {
        // Before selection retain the legacy sample; selected templates use the shared naming preview.
        HomeSaveInfoSheet(
            fileName = if (selected) savedPreview.previewNaming.displayName else "ios-sample_0001.jpg",
            relativePaths = if (selected) listOf("사진 저장 연결 전") else listOf("PoC/현장 기록/", "PoC/보관/"),
            onOpenSaveSettings = {}, onDismiss = { showSaveInfo = false },
            settingsEnabled = false,
        )
    }
}

/** Same PNG as Android, loaded once from the native bundle; all rendering stays shared. */
@OptIn(ExperimentalForeignApi::class)
@Composable
private fun rememberHomeLeafPainter(): Painter {
    val bitmap = remember {
        val path = requireNotNull(NSBundle.mainBundle.pathForResource("home_leaf_shadow_c", "png"))
        val data = requireNotNull(NSData.dataWithContentsOfFile(path))
        val bytes = requireNotNull(data.bytes).reinterpret<ByteVar>().readBytes(data.length.toInt())
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }
    return remember(bitmap) { BitmapPainter(bitmap) }
}
