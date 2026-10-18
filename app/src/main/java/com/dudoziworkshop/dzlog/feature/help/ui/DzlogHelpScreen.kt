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
    onOpenLiveSession: (String, String) -> Unit = { _, _ -> },
) {
    val area = moduleId?.let(DzlogTutorialSections::area)
    if (area == null) {
        BackHandler { onBack() }
        HelpIndex(onOpenModule, onOpenIntroduction, onBack)
    } else {
        BackHandler { onBack() }
        SessionIndex(area, onBack) { onOpenLiveSession(area.id, area.sessions[it].id) }
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
            Text("알아보고 싶은 기능을 골라 살펴보세요.",
                style = DDZTypography.Body, color = DDZColor.TextSecondary)
            DzlogTutorialSections.areas.forEach { area ->
                HelpCard(area.title, when (area.id) {
                    "home" -> "촬영 준비 · 사진 확인 · 편집과 설정"
                    "camera" -> "촬영과 확인 · 촬영도구 · 표와 빠른 수정"
                    "gallery" -> "사진과 폴더 찾기 · 보기와 관리"
                    "table-detail" -> "표 내용 · 구조 · 스타일 바꾸기"
                    "saving" -> "파일명 · 저장 위치 · 자동번호"
                    else -> "촬영 환경 · 권한 · 백업과 도움말"
                }, { onOpenModule(area.id) })
            }
            TextButton(onClick = onOpenIntroduction) { Text("처음 사용법 다시 보기") }
        }
    }
}

@Composable
private fun SessionIndex(area: TutorialArea, onBack: () -> Unit, onSelect: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val progress = remember { TutorialProgressStore(context) }
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = area.title, onBack = onBack)
    }) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets)
                .verticalScroll(rememberScrollState()).padding(DDZSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("원하는 설명부터 볼 수 있어요.",
                style = DDZTypography.Body, color = DDZColor.TextSecondary)
            area.sessions.forEachIndexed { index, session ->
                HelpCard(session.title, "${session.steps.sumOf { 1 + it.scenes.size }}개 설명 · ${progress.status(area.id, session.id)}",
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
