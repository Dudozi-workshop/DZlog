package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.DzlogTutorialSections
import com.dudoziworkshop.dzlog.feature.help.model.TutorialArea
import com.dudoziworkshop.dzlog.feature.help.model.TutorialSession
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Read-only guide. Neither browsing nor jumping to a session modifies application data. */
@Composable
internal fun DzlogHelpScreen(
    moduleId: String?,
    onOpenModule: (String) -> Unit,
    onOpenIntroduction: () -> Unit,
    onBack: () -> Unit,
) {
    val area = moduleId?.let(DzlogTutorialSections::area)
    if (area == null) {
        BackHandler { onBack() }
        HelpIndex(onOpenModule, onOpenIntroduction, onBack)
    } else {
        var selectedSession by rememberSaveable(area.id) { mutableIntStateOf(-1) }
        BackHandler { if (selectedSession >= 0) selectedSession = -1 else onBack() }
        if (selectedSession < 0) {
            SessionIndex(area, onBack) { selectedSession = it }
        } else {
            val safe = selectedSession.coerceIn(area.sessions.indices)
            TutorialReader(area, safe,
                onChooseSession = { selectedSession = it },
                onBack = { selectedSession = -1 })
        }
    }
}

@Composable
private fun HelpIndex(
    onOpenModule: (String) -> Unit,
    onOpenIntroduction: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = "사용법 및 도움말", onBack = onBack)
    }) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets)
                .verticalScroll(rememberScrollState()).padding(DDZSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("알아보고 싶은 구역을 선택해 세션별 기능을 살펴보세요.",
                style = DDZTypography.Body, color = DDZColor.TextSecondary)
            HelpCard("처음 사용법 다시 보기", "설정을 변경하지 않고 소개 화면을 다시 볼 수 있어요.", onOpenIntroduction)
            DzlogTutorialSections.areas.forEach { area ->
                HelpCard(area.title, "${area.sessions.size}개 세션", { onOpenModule(area.id) })
            }
        }
    }
}

@Composable
private fun SessionIndex(area: TutorialArea, onBack: () -> Unit, onSelect: (Int) -> Unit) {
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = area.title, onBack = onBack)
    }) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets)
                .verticalScroll(rememberScrollState()).padding(DDZSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("원하는 세션으로 바로 이동할 수 있어요.",
                style = DDZTypography.Body, color = DDZColor.TextSecondary)
            area.sessions.forEachIndexed { index, session ->
                HelpCard("세션 ${index + 1} · ${session.title}", "${session.steps.size}개 항목",
                    { onSelect(index) })
            }
        }
    }
}

@Composable
private fun HelpCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = DDZColor.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DDZColor.Border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.HelpOutline, null, tint = DDZColor.PrimaryDark)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Text(subtitle, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = DDZColor.IconMuted)
        }
    }
}

private data class TutorialPage(val stepIndex: Int, val sceneIndex: Int?)

@Composable
private fun TutorialReader(
    area: TutorialArea,
    sessionIndex: Int,
    onChooseSession: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val session: TutorialSession = area.sessions[sessionIndex]
    val pages = remember(area.id, session.id) {
        session.steps.flatMapIndexed { index, step ->
            listOf(TutorialPage(index, null)) +
                step.scenes.indices.map { sceneIndex -> TutorialPage(index, sceneIndex) }
        }
    }
    var pageIndex by rememberSaveable(area.id, session.id) { mutableIntStateOf(0) }
    var showContents by rememberSaveable(area.id, session.id) { mutableIntStateOf(0) }
    val safeIndex = pageIndex.coerceIn(0, pages.lastIndex)
    val page = pages[safeIndex]
    val step = session.steps[page.stepIndex]
    val scene = page.sceneIndex?.let(step.scenes::get)
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = area.title + " · " + session.title, onBack = onBack)
    }) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(DDZSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("세션 ${sessionIndex + 1}/${area.sessions.size} · 설명 ${safeIndex + 1}/${pages.size}",
                    style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                Text(scene?.title ?: step.title,
                    style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
                Surface(
                    color = DDZColor.Surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DDZColor.Border),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(scene?.instruction ?: step.instruction,
                            style = DDZTypography.Body, color = DDZColor.TextPrimary)
                    }
                }
                Text("기능 안내만 표시하며 사진·설정·저장 내용은 변경하지 않아요.",
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                TextButton(onClick = { showContents = if (showContents == 0) 1 else 0 }) {
                    Text(if (showContents == 0) "세션 목차 · 바로 이동" else "목차 닫기",
                        color = DDZColor.PrimaryDark)
                }
                if (showContents == 1) {
                    area.sessions.forEachIndexed { index, item ->
                        HelpCard("세션 ${index + 1} · ${item.title}",
                            if (index == sessionIndex) "현재 세션" else "바로 이동",
                            { onChooseSession(index) })
                    }
                    session.steps.forEachIndexed { index, item ->
                        HelpCard("설명 ${index + 1} · ${item.title}", "이 설명으로 이동",
                            { pageIndex = pages.indexOfFirst { it.stepIndex == index && it.sceneIndex == null }; showContents = 0 })
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(DDZSpacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = safeIndex > 0,
                    onClick = { pageIndex = safeIndex - 1 },
                ) { Text("이전", color = DDZColor.PrimaryDark) }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (safeIndex < pages.lastIndex) pageIndex = safeIndex + 1
                        else if (sessionIndex < area.sessions.lastIndex) onChooseSession(sessionIndex + 1)
                        else onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
                ) {
                    Text(if (safeIndex < pages.lastIndex) "다음"
                        else if (sessionIndex < area.sessions.lastIndex) "다음 세션" else "세션 목록")
                }
            }
        }
    }
}
