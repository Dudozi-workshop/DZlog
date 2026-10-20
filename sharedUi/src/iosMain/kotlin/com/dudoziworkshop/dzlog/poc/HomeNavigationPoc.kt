package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.ui.home.*
import com.dudoziworkshop.dzlog.ui.table.template.TableTemplateListContent
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.Foundation.*

/** Memory-only navigation host. The home, list, empty photo state and animation are shared UI. */
@Composable
internal fun HomeNavigationPoc(template: SavedTableTemplate) {
    var showList by remember { mutableStateOf(false) }
    var showSaveInfo by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(false) }
    val leaf = rememberHomeLeafPainter()
    if (showList) {
        TableTemplateListContent(
            templates = listOf(template), activeTemplateId = if (selected) template.id else null,
            onBack = { showList = false }, onSelectTemplate = { selected = true; showList = false },
            allowManagement = false,
            onOpenTemplate = {}, onCreateTemplate = {}, onRenameTemplate = { _, _ -> },
            onDuplicateTemplate = {}, onDeleteTemplate = {},
            formatDate = { _, used -> if (used) "사용 기록 없음" else "수정일 없음" },
        )
    } else {
        HomeContent(
            templateName = if (selected) template.name else "기본 촬영",
            labels = HomeLabels("설정", "현재 템플릿", "다음 촬영 번호 0001", "촬영 시작", "표 편집", "저장 정보"),
            onManageTemplates = { showList = true }, onOpenSettings = {}, onStartCamera = {},
            onOpenTableEditor = {}, onOpenSaveInfo = { showSaveInfo = true },
            modifier = Modifier.fillMaxSize().background(DDZColor.Background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
            captureEnabled = false, settingsEnabled = false, editingEnabled = false, saveInfoEnabled = true,
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
    if (showSaveInfo) {
        // Synthetic display data only: no Photos/MediaStore destination or save side effect.
        HomeSaveInfoSheet(
            fileName = "ios-sample_0001.jpg",
            relativePaths = listOf("PoC/현장 기록/", "PoC/보관/"),
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
