package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.activity.compose.BackHandler
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
    val session = area.sessions.first { it.id == sessionId }
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
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        if (contents) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
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
            Box(Modifier.weight(1f).fillMaxWidth().clearAndSetSemantics { contentDescription = "기능 안내 화면" }) {
                CompositionLocalProvider(
                    LocalTutorialLocation provides location,
                    LocalTutorialTarget provides tutorialTarget(location, page.parentId),
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
        Surface(color = DDZColor.Surface, shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${area.title} · 세션 ${area.sessions.indexOf(session) + 1}/${area.sessions.size} · ${index + 1}/${pages.size}",
                        Modifier.weight(1f), style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                    TextButton(onClick = onClose) { Text("닫기") }
                }
                Text(session.title, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                Text(page.title, style = DDZTypography.SettingLabel, color = DDZColor.PrimaryDark)
                Text(page.instruction, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                if (location.areaId == "gallery") Text("저장된 사진과 폴더가 있는 경우 해당 항목이 보여요.",
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pageId = pages[index - 1].id }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("이전") }
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
        "recent" -> "home.recent"; "table-settings" -> "home.table"; "save-info" -> "home.saving"
        "start" -> "home.start"; else -> "home.main"
    }
    "camera" -> when (parent) {
        "flash", "grid", "table", "assist", "focus" -> "camera.tool.$parent"
        "menu" -> "camera.tools"
        "zoom" -> "camera.zoom"
        "entry" -> "camera.settings"
        "quality" -> "camera.settings.저장"
        "feedback" -> "camera.settings.피드백"
        "controls" -> "camera.settings.음량키"
        "quick", "template" -> "camera.quick"
        "undo", "warning" -> "camera.undo"
        "recent", "result", "preview" -> "camera.recent"
        "shutter", "saving", "bottom" -> "camera.shutter"
        "top", "filename", "path", "counter" -> "camera.filename"
        else -> "camera.preview"
    }
    "settings" -> when {
        parent == "warning" -> "settings.other.저장경로 변경 시 삭제 경고"
        parent == "help" -> "settings.other.사용법 및 도움말"
        location.sessionId == "permission" -> "settings.permission"
        location.sessionId == "data" -> "settings.data"
        location.sessionId == "feedback" -> "settings.feedback"
        else -> "settings.capture"
    }
    "gallery" -> "gallery.content"
    "saving" -> if (location.sessionId == "rotation") "table.panel" else "saving.detail"
    "table-detail" -> when (location.sessionId) { "overview" -> "table.tabs"; "save" -> "table.save"; else -> "table.panel" }
    else -> "table.panel"
}
