package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.*
import com.dudoziworkshop.dzlog.ui.theme.*

/** Only the guide controls receive touch/accessibility actions. The actual screen is passive. */
@Composable
internal fun LiveTutorialHost(
    initialAreaId: String,
    initialSessionId: String,
    onClose: () -> Unit,
    screen: @Composable (TutorialLocation) -> Unit,
) {
    val context = LocalContext.current
    val progress = remember { TutorialProgressStore(context) }
    var areaId by rememberSaveable { mutableStateOf(initialAreaId) }
    var sessionId by rememberSaveable { mutableStateOf(initialSessionId) }
    val area = requireNotNull(DzlogTutorialSections.area(areaId))
    val session = area.sessions.firstOrNull { it.id == sessionId } ?: area.sessions.first()
    LaunchedEffect(session.id) { if (sessionId != session.id) sessionId = session.id }
    val pages = remember(areaId, sessionId) { session.guidePages() }
    var pageId by rememberSaveable(areaId, sessionId) { mutableStateOf(progress.page(areaId, sessionId)) }
    var contents by rememberSaveable { mutableStateOf(false) }
    val index = tutorialPageIndex(pages, pageId)
    val page = pages[index]
    val location = TutorialLocation(areaId, sessionId, page.id)
    LaunchedEffect(location) { progress.save(location) }
    fun select(area: String, session: String) {
        areaId = area
        sessionId = session
        contents = false
    }
    var replay by remember(location) { mutableIntStateOf(0) }
    val demo = remember { Animatable(0f) }
    val hasDemo = (areaId == "camera" && page.id in TutorialDemoPages) ||
        (areaId == "table-detail" && sessionId == "style" && page.id.startsWith("style/"))
    LaunchedEffect(location, replay) {
        demo.snapTo(0f)
        if (hasDemo) { delay(800); demo.animateTo(1f, tween(if (page.id in setOf("preview", "ratio", "quality", "capture-confirmation", "feedback", "controls")) 4200 else 2400)) }
    }
    var targetBounds by remember(location) { mutableStateOf<Rect?>(null) }
    var rootY by remember { mutableFloatStateOf(0f) }
    var cardHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val areaPages = area.sessions.flatMap { it.guidePages() }
    val overallIndex = area.sessions.take(area.sessions.indexOf(session)).sumOf { it.guidePages().size } + index
    BoxWithConstraints(Modifier.fillMaxSize().navigationBarsPadding().onGloballyPositioned { rootY = it.positionInRoot().y }) {
        if (contents) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                TextButton(onClick = { contents = false }) { Text("안내로 돌아가기") }
                Text("목차 · 바로 이동", style = DDZTypography.SettingLabel)
                DzlogTutorialSections.areas.forEach { item ->
                    Text(item.title, modifier = Modifier.padding(top = 12.dp), style = DDZTypography.SettingLabel)
                    item.sessions.forEachIndexed { i, section ->
                        TextButton(onClick = { select(item.id, section.id) }) {
                            Text("${i + 1}. ${section.title} · ${progress.status(item.id, section.id)}")
                        }
                    }
                }
                Text("현재 세션의 설명", style = DDZTypography.SettingLabel)
                pages.forEachIndexed { i, entry ->
                    TextButton(onClick = { pageId = entry.id; contents = false }) { Text("${i + 1}. ${entry.title}") }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().clearAndSetSemantics { contentDescription = "기능 안내 화면" }) {
                CompositionLocalProvider(
                    LocalTutorialBounds provides { targetBounds = it },
                    LocalTutorialDemoProgress provides demo.value,
                    LocalTutorialLocation provides location,
                    LocalTutorialTarget provides (if (location.areaId == "camera" && page.id == "template") "camera.quick-table" else tutorialTarget(location, page.parentId)),
                    LocalTutorialReadOnly provides true,
                ) { screen(location) }
                if (areaId == "camera" && page.id == "pinch") {
                    TutorialPinchHint(Modifier.align(Alignment.Center))
                }
                // A sibling shield also covers AndroidView camera preview touch events.
                Box(Modifier.matchParentSize().pointerInput(Unit) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                })
            }
        }
        if (!contents) Canvas(Modifier.fillMaxSize()) {
            val shade = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
            val target = targetBounds?.translate(0f, -rootY)?.inflate(5.dp.toPx())
            val cutout = Path().apply { if (target != null) addRoundRect(androidx.compose.ui.geometry.RoundRect(target, CornerRadius(12.dp.toPx()))) }
            drawPath(Path.combine(PathOperation.Difference, shade, cutout), Color.Black.copy(alpha = 0.48f))
        }
        val gap = with(density) { 16.dp.toPx() }
        val hostHeight = with(density) { maxHeight.toPx() }
        val target = targetBounds?.translate(0f, -rootY)
        val below = target == null || target.center.y < hostHeight / 2
        val desiredY = if (below) (target?.bottom ?: 0f) + gap else (target?.top ?: hostHeight) - cardHeight - gap
        val topY = (WindowInsets.statusBars.getTop(density) - rootY).coerceAtLeast(0f) + gap
        val bottomY = (hostHeight - cardHeight - gap).coerceAtLeast(topY)
        // Keep consecutive explanations anchored, rather than jumping around their targets.
        val cardY = when {
            areaId == "home" && page.id in setOf("recent", "album") -> topY
            areaId == "home" && page.id in setOf("main", "templates", "manage", "more") -> bottomY
            else -> desiredY.coerceIn(topY, bottomY)
        }
        if (!contents) Surface(modifier = Modifier.padding(horizontal = 16.dp).offset { IntOffset(0, cardY.roundToInt()) }
            .onGloballyPositioned { cardHeight = it.size.height },
            shape = RoundedCornerShape(18.dp), color = DDZColor.Surface, shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${area.title} · ${session.title}   ${overallIndex + 1}/${areaPages.size}",
                        Modifier.weight(1f), style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                    if (hasDemo) TextButton(onClick = { replay++ }) { Text("다시 보기") }
                    TextButton(onClick = onClose) { Text("닫기") }
                }
                LinearProgressIndicator(progress = { (overallIndex + 1).toFloat() / areaPages.size }, modifier = Modifier.fillMaxWidth(), color = DDZColor.PrimaryDark)
                Text(page.title, style = DDZTypography.SettingLabel, color = DDZColor.PrimaryDark)
                Text(page.instruction, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                if (location.areaId == "gallery") Text("저장된 사진과 폴더가 있는 경우 해당 항목이 보여요.",
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (index > 0) pageId = pages[index - 1].id
                        else { val previous = area.sessions[area.sessions.indexOf(session) - 1]; progress.save(TutorialLocation(areaId, previous.id, previous.guidePages().last().id)); select(areaId, previous.id) }
                    }, enabled = overallIndex > 0, modifier = Modifier.weight(1f)) { Text("이전") }
                    OutlinedButton(onClick = { contents = !contents }, modifier = Modifier.weight(1f)) { Text(if (contents) "안내" else "목차") }
                    Button(onClick = {
                        if (index < pages.lastIndex) pageId = pages[index + 1].id
                        else {
                            progress.complete(location)
                            val next = area.sessions.indexOf(session) + 1
                            if (next <= area.sessions.lastIndex) select(areaId, area.sessions[next].id)
                            else contents = true
                        }
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark)) {
                        Text(if (index < pages.lastIndex) "다음" else "완료")
                    }
                }
            }
        }
    }
    // Registered after the child screens so device Back cannot perform normal screen actions.
    BackHandler { if (contents) contents = false else onClose() }
}

private fun tutorialTarget(location: TutorialLocation, parent: String): String = when (location.areaId) {
    "home" -> when (parent) {
        "templates" -> "home.template-list"
        "manage" -> "home.template-more"
        "save-info" -> "home.saving"
        "recent" -> "home.photo"; "album" -> "home.album"; "more" -> "home.template-menu"; "counter" -> "home.counter"; "settings" -> "home.settings"; "table-settings" -> "home.table"
        "start" -> "home.start"; else -> "home.main"
    }
    "camera" -> when (parent) {
        "flash", "manual-focus" -> "camera.tool-detail"
        "grid", "table", "assist" -> "camera.tool.$parent"
        "focus" -> "camera.preview"
        "menu" -> "camera.tools"
        "zoom" -> "camera.zoom"
        "select", "lock" -> "camera.table-toolbar"
        "move", "size", "rotation" -> "camera.watermark"
        "details" -> "table.panel"
        "entry" -> "camera.settings"
        "ratio" -> "camera.settings.비율"
        "capture-confirmation" -> "camera.settings.촬영 확인"
        "quality" -> "camera.settings.저장"
        "feedback" -> "camera.settings.피드백"
        "controls" -> "camera.settings.음량키"
        "quick", "quick-filename", "quick-path" -> "camera.quick-naming"
        "quick-text", "quick-number", "quick-date", "quick-time", "quick-phrase" -> "camera.quick-editor"
        "template" -> "camera.quick-naming"
        "undo" -> "camera.undo"
        "warning" -> "camera.undo-warning"
        "recent" -> "camera.recent"
        "result" -> "camera.results"
        "preview" -> "camera.capture-confirmation"
        "shutter", "bottom" -> "camera.shutter"
        "saving" -> "camera.save-detail"
        "top" -> "camera.filename"
        "filename", "path", "counter" -> "camera.next-info"
        else -> "camera.preview"
    }
    "settings" -> when {
        location.pageId == "permission-screen" -> "settings.screen"
        location.pageId == "export-screen" -> "settings.backup.저장 위치 선택"
        location.pageId == "import-screen" -> "settings.backup.다른 백업 파일 선택"
        location.pageId == "import" -> "settings.other.작업 환경 가져오기"
        location.pageId == "preview" -> "settings.quality-options"
        parent == "warning" -> "settings.other.저장경로 변경 시 삭제 경고"
        parent == "help" -> "settings.other.사용법 및 도움말"
        location.sessionId == "permission" -> "settings.permission"
        location.sessionId == "data" -> "settings.data"
        location.sessionId == "feedback" -> "settings.feedback"
        else -> "settings.capture"
    }
    "gallery" -> when {
        location.sessionId == "viewer" -> "gallery.viewer-actions"
        location.pageId in setOf("photos/move", "photos/delete", "folders/new", "folders/rename", "folders/move", "folders/delete") -> "guide.dialog"
        location.sessionId == "photos" && location.pageId != "photos/select" -> "gallery.selection-actions"
        parent == "browse" -> "gallery.breadcrumbs"
        else -> "gallery.content"
    }
    "saving" -> when (location.pageId) {
        "filename/copy", "folder/copy", "counter/input", "counter/reset" -> "guide.dialog"
        "filename/preview" -> "saving.preview.현재 파일명"
        "folder" -> "saving.preview.현재 저장 위치"
        "filename/sources" -> "saving.add"
        "filename/parts", "filename/edit", "filename/order", "filename/remove", "folder/parts" -> "saving.rules"
        "counter/next" -> "saving.next"
        "counter/digits" -> "saving.digits"
        "counter/sync" -> "saving.sync"
        else -> if (location.sessionId == "rotation") "table.panel" else "saving.detail"
    }
    "table-detail" -> when (location.sessionId) {
        "overview" -> "table.tabs"
        "save" -> when (location.pageId) {
            "save/decision" -> "guide.dialog"
            "save/undo" -> "table.history.실행 취소"
            "save/redo" -> "table.history.다시 실행"
            else -> "table.save"
        }
        "structure" -> if ('/' in location.pageId) "table.structure.${location.pageId.substringAfter('/')}" else "table.panel"
        "style" -> when (location.pageId.substringAfter('/')) {
            "background" -> "table.style.배경"
            "opacity" -> "table.style.배경 투명도"
            "text" -> "table.style.글자 크기"
            "alignment" -> "table.style.정렬"
            "border" -> "table.style.테두리"
            else -> "table.panel"
        }
        else -> "table.panel"
    }
    else -> "table.panel"
}
