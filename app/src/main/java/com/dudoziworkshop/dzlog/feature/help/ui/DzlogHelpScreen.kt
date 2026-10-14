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
import androidx.compose.foundation.layout.weight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.DzlogTutorialCatalog
import com.dudoziworkshop.dzlog.feature.help.model.TutorialModule
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Safe, read-only help UI. Content never executes the described camera/media operations. */
@Composable
internal fun DzlogHelpScreen(
    moduleId: String?,
    onOpenModule: (String) -> Unit,
    onOpenIntroduction: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    val module = moduleId?.let(DzlogTutorialCatalog::module)
    if (module == null) {
        HelpIndex(onOpenModule, onOpenIntroduction, onBack)
    } else {
        TutorialReader(module, onBack)
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
            Text("DZlog의 주요 기능을 순서대로 살펴볼 수 있어요.",
                style = DDZTypography.Body, color = DDZColor.TextSecondary)
            HelpCard(
                title = "처음 사용법 다시 보기",
                subtitle = "최초 안내를 다시 살펴봐요. 기존 설정은 변경되지 않아요.",
                onClick = onOpenIntroduction,
            )
            Text("기능별 사용법", style = DDZTypography.SettingLabel,
                color = DDZColor.PrimaryDark,
                modifier = Modifier.padding(top = 8.dp))
            DzlogTutorialCatalog.modules.forEach { module ->
                HelpCard(module.title, module.steps.size.toString() + "단계", onClick = {
                    onOpenModule(module.id)
                })
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
private fun TutorialReader(module: TutorialModule, onBack: () -> Unit) {
    val pages = remember(module.id) {
        module.steps.flatMapIndexed { index, step ->
            listOf(TutorialPage(index, null)) +
                step.scenes.indices.map { sceneIndex -> TutorialPage(index, sceneIndex) }
        }
    }
    var pageIndex by rememberSaveable(module.id) { mutableIntStateOf(0) }
    val safeIndex = pageIndex.coerceIn(0, pages.lastIndex)
    val page = pages[safeIndex]
    val step = module.steps[page.stepIndex]
    val scene = page.sceneIndex?.let(step.scenes::get)
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = module.title, onBack = onBack)
    }) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(DDZSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "단계 " + (page.stepIndex + 1) + "/" + module.steps.size +
                        (page.sceneIndex?.let { "  ·  세부 " + (it + 1) + "/" + step.scenes.size } ?: ""),
                    style = DDZTypography.Caption, color = DDZColor.PrimaryDark,
                )
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
                        if (scene == null && step.scenes.isNotEmpty()) {
                            Text(
                                "이 단계에는 " + step.scenes.size + "개의 자세한 설명이 있어요.",
                                style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                            )
                        }
                    }
                }
                Text(
                    "읽기 전용 안내입니다. 촬영·저장·삭제·설정 변경은 수행하지 않아요.",
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                )
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
                    onClick = { if (safeIndex == pages.lastIndex) onBack() else pageIndex = safeIndex + 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
                ) { Text(if (safeIndex == pages.lastIndex) "목록으로" else "다음") }
            }
        }
    }
}
