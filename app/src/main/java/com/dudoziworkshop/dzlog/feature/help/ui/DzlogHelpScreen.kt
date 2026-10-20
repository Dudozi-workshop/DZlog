package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.*
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.*

/** Only guide preferences are written; app settings, templates and photos stay untouched. */
@Composable
internal fun DzlogHelpScreen(
    moduleId: String?,
    onOpenModule: (String) -> Unit,
    onOpenIntroduction: () -> Unit,
    onBack: () -> Unit,
    onOpenLiveSession: (String, String) -> Unit = { _, _ -> },
) {
    val area = moduleId?.let(DzlogTutorialSections::area)
    BackHandler { onBack() }
    if (area == null) HelpIndex(onOpenModule, onOpenIntroduction, onBack)
    else SessionIndex(area, onBack) { onOpenLiveSession(area.id, it) }
}

private fun areaIcon(id: String): ImageVector = when (id) {
    "home" -> Icons.Default.Home
    "camera" -> Icons.Default.PhotoCamera
    "gallery" -> Icons.Default.PhotoLibrary
    "table-detail" -> Icons.Default.GridOn
    "saving" -> Icons.Default.Folder
    else -> Icons.Default.Settings
}

private fun areaDescription(id: String) = when (id) {
    "home" -> "템플릿과 주요 메뉴"
    "camera" -> "촬영과 표 조작"
    "gallery" -> "사진과 폴더 관리"
    "table-detail" -> "내용·구조·스타일"
    "saving" -> "파일명·저장경로·촬영번호"
    else -> "촬영 환경·권한·백업"
}

private fun sessionDescription(area: TutorialArea, session: TutorialSession): String =
    if (area.id == "home") when (session.id) {
        "overview" -> "템플릿 선택·관리·촬영 시작"
        "recent" -> "최근 촬영·앨범 보기"
        else -> "표 편집·저장 정보·앱 설정"
    } else "${session.guidePages().size}개 설명"

@Composable
private fun HelpIndex(onOpenModule: (String) -> Unit, onOpenIntroduction: () -> Unit, onBack: () -> Unit) {
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = "사용법 및 도움말", onBack = onBack)
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
            .padding(DDZSpacing.screenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("궁금한 기능을 살펴보세요.", Modifier.padding(bottom = 8.dp),
                style = DDZTypography.Body, color = DDZColor.TextSecondary)
            DzlogTutorialSections.areas.forEach { area ->
                HelpCard(area.title, areaDescription(area.id), areaIcon(area.id), onClick = { onOpenModule(area.id) })
            }
            TextButton(onClick = onOpenIntroduction, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("처음 사용법 다시 보기", color = DDZColor.PrimaryDark)
            }
        }
    }
}

@Composable
private fun SessionIndex(area: TutorialArea, onBack: () -> Unit, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val progress = remember { TutorialProgressStore(context) }
    val resume = progress.resume(area)
    val started = area.sessions.any { progress.page(area.id, it.id).isNotEmpty() }
    val completed = area.sessions.all { progress.status(area.id, it.id) == "완료" }
    val total = area.sessions.sumOf { it.guidePages().size }
    val current = if (!started) 0 else if (completed) total else
        area.sessions.takeWhile { it.id != resume.sessionId }.sumOf { it.guidePages().size } +
            tutorialPageIndex(area.sessions.first { it.id == resume.sessionId }.guidePages(), resume.pageId) + 1
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = "${area.title} 안내", onBack = onBack)
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
            .padding(DDZSpacing.screenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${area.title}의 주요 기능을 알아보세요.", style = DDZTypography.Body, color = DDZColor.TextSecondary)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("${area.title} 안내 진행도", Modifier.weight(1f), style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                Text("$current / $total", style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
            }
            LinearProgressIndicator(progress = { current.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(4.dp),
                color = DDZColor.PrimaryDark, trackColor = DDZColor.SelectedSoft)
            Button(onClick = {
                if (!started || completed) { progress.start(area); onSelect(area.sessions.first().id) }
                else onSelect(resume.sessionId)
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark)) {
                Text(if (completed) "다시 보기" else if (started) "이어서 보기" else "처음부터 보기")
            }
            if (started && !completed) TextButton(onClick = { progress.start(area); onSelect(area.sessions.first().id) },
                modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("처음부터 보기", color = DDZColor.PrimaryDark) }
            Text("목차", Modifier.padding(top = 12.dp), style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            area.sessions.forEach { session ->
                HelpCard(session.title, sessionDescription(area, session), when (session.id) {
                    "overview", "capture" -> areaIcon(area.id)
                    "recent" -> Icons.Default.PhotoLibrary
                    else -> Icons.Default.Settings
                }, status = progress.status(area.id, session.id).takeUnless { it == "미시작" },
                    onClick = { onSelect(session.id) })
            }
        }
    }
}

@Composable
private fun HelpCard(title: String, subtitle: String, icon: ImageVector, status: String? = null, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = if (status == "보는 중") DDZColor.SelectedSoft.copy(alpha = 0.35f) else DDZColor.Surface,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, DDZColor.Border)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = DDZColor.SelectedSoft) {
                Icon(icon, null, Modifier.padding(10.dp).size(24.dp), tint = DDZColor.PrimaryDark)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
                Text(subtitle, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
            when (status) {
                "완료" -> Icon(Icons.Default.Check, "완료", tint = DDZColor.PrimaryDark)
                "보는 중" -> Surface(shape = RoundedCornerShape(20.dp), color = DDZColor.SelectedSoft) {
                    Text(status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                }
                else -> Icon(Icons.Default.ChevronRight, null, tint = DDZColor.IconMuted)
            }
        }
    }
}

/** Shared tutorial contents: compact sessions, progress and explicit restart/resume. */
@Composable
internal fun TutorialContentsScreen(
    area: TutorialArea,
    location: TutorialLocation,
    progress: TutorialProgressStore,
    onResume: () -> Unit,
    onJump: (TutorialLocation) -> Unit,
    onArea: (TutorialArea) -> Unit,
) {
    var otherAreas by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (otherAreas) otherAreas = false else onResume() }
    val total = area.sessions.sumOf { it.guidePages().size }.coerceAtLeast(1)
    val viewed = area.sessions.sumOf { session ->
        session.guidePages().count { page ->
            progress.viewed(TutorialLocation(area.id, session.id, page.id))
        }
    }
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(
            title = if (otherAreas) "다른 안내 보기" else "${area.title} 목차",
            onBack = { if (otherAreas) otherAreas = false else onResume() },
        )
    }) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = DDZSpacing.screenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (otherAreas) {
                DzlogTutorialSections.areas.forEach { item ->
                    HelpCard(
                        item.title, areaDescription(item.id), areaIcon(item.id),
                        onClick = { otherAreas = false; onArea(item) },
                    )
                }
            } else {
                Surface(
                    color = DDZColor.SurfaceSoft,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${area.title} 기능 살펴보기",
                                Modifier.weight(1f), style = DDZTypography.SettingLabel,
                                color = DDZColor.TextPrimary,
                            )
                            Text(
                                "$viewed / $total",
                                style = DDZTypography.Caption, color = DDZColor.PrimaryDark,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { viewed.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = DDZColor.PrimaryDark,
                            trackColor = DDZColor.SelectedSoft,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val first = area.sessions.first()
                                    progress.start(area)
                                    onJump(TutorialLocation(area.id, first.id, first.guidePages().first().id))
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) {
                                Text("처음부터 진행")
                            }
                            Button(
                                onClick = { onJump(progress.resume(area)) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
                            ) {
                                Text("이어서 진행")
                            }
                        }
                    }
                }
                Text(
                    "세션 ${area.sessions.size}개 · 원하는 설명을 선택하세요.",
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                )
                area.sessions.forEach { session ->
                    var expanded by rememberSaveable(area.id, session.id, location.sessionId) {
                        mutableStateOf(session.id == location.sessionId)
                    }
                    val pages = session.guidePages()
                    val seen = pages.count { page ->
                        progress.viewed(TutorialLocation(area.id, session.id, page.id))
                    }
                    val currentSession = session.id == location.sessionId
                    Surface(
                        color = DDZColor.Surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (currentSession) DDZColor.PrimaryDark else DDZColor.Border,
                        ),
                    ) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth().clickable { expanded = !expanded }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (currentSession) {
                                    Icon(
                                        Icons.Default.PlayArrow, contentDescription = null,
                                        tint = DDZColor.PrimaryDark,
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else if (progress.status(area.id, session.id) == "완료") {
                                    Icon(
                                        Icons.Default.CheckCircle, contentDescription = null,
                                        tint = DDZColor.PrimaryDark,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    session.title, Modifier.weight(1f),
                                    style = DDZTypography.SettingLabel,
                                    color = DDZColor.TextPrimary,
                                )
                                Text(
                                    if (progress.status(area.id, session.id) == "완료") "완료"
                                    else "$seen/${pages.size}",
                                    style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                                )
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (expanded) "접기" else "펼치기",
                                    tint = DDZColor.IconMuted, modifier = Modifier.size(20.dp),
                                )
                            }
                            if (expanded) pages.forEach { page ->
                                val entry = TutorialLocation(area.id, session.id, page.id)
                                val current = entry == location
                                HorizontalDivider(color = DDZColor.Border.copy(alpha = 0.5f))
                                Surface(color = if (current) DDZColor.SelectedSoft else DDZColor.Surface) {
                                    Row(
                                        Modifier.fillMaxWidth().clickable { onJump(entry) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            page.title, Modifier.weight(1f),
                                            style = DDZTypography.Body,
                                            color = if (current) DDZColor.PrimaryDark else DDZColor.TextPrimary,
                                        )
                                        when {
                                            current -> Text(
                                                "현재", style = DDZTypography.Caption,
                                                color = DDZColor.PrimaryDark,
                                            )
                                            progress.viewed(entry) -> Icon(
                                                Icons.Default.Check, "확인한 설명",
                                                tint = DDZColor.PrimaryDark,
                                            )
                                            else -> Icon(
                                                Icons.Default.ChevronRight, null,
                                                tint = DDZColor.IconMuted,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { otherAreas = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("다른 안내 보기", color = DDZColor.PrimaryDark)
                }
            }
        }
    }
}
