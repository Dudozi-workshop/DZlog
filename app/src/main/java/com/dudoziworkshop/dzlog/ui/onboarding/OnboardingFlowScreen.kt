package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterDefinition
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.naming.buildFileName
import com.dudoziworkshop.dzlog.domain.naming.buildSavePath
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import java.util.Date

private data class StarterCategory(val id: String, val title: String)
private val categories = listOf(
    StarterCategory("basic", "기본 촬영"),
    StarterCategory("daily", "일상·취미"),
    StarterCategory("work", "업무·현장"),
)

@Composable
internal fun OnboardingFlowScreen(
    busy: Boolean,
    onChoose: (String) -> Unit,
    onSkip: () -> Unit,
    readOnly: Boolean = false,
    onClose: () -> Unit = {},
) {
    var step by remember { mutableIntStateOf(1) }
    BackHandler(enabled = readOnly) {
        if (step > 1) step-- else onClose()
    }
    var category by remember { mutableStateOf("basic") }
    var chosen by remember { mutableStateOf(OnboardingStarterCatalog.DEFAULT_ID) }
    val selected = OnboardingStarterCatalog.find(chosen)
        ?: OnboardingStarterCatalog.definitions.first()

    Column(
        Modifier.fillMaxSize().background(DDZColor.Background).statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DZlog", color = DDZColor.PrimaryDark, fontWeight = FontWeight.Bold)
            Text("$step / 3", color = DDZColor.TextSecondary)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                1 -> {
                    Spacer(Modifier.height(18.dp))
                    Text("찍고, 기록하고,\n자동으로 정리하다.",
                        color = DDZColor.TextPrimary, fontWeight = FontWeight.Bold,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    Text("사진 위에 기록표를 더하고, 파일명과 폴더까지 자동으로 정리해요.",
                        color = DDZColor.TextSecondary)
                    IntroTile("01", "사진을 찍어요", "평소처럼 촬영하면 돼요.")
                    IntroTile("02", "표로 기록해요", "내용은 촬영 화면에서 빠르게 수정할 수 있어요.")
                    IntroTile("03", "자동으로 저장해요", "원본과 기록사진을 함께 보관해요.")
                }
                2 -> {
                    Text("사진 한 장으로\n정리까지 끝내요.",
                        color = DDZColor.TextPrimary, fontWeight = FontWeight.Bold,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    IntroTile("1", "사진 촬영", "촬영 화면 위에 기록표가 표시돼요.")
                    IntroTile("2", "필요한 정보만 수정", "기록명·대상·구분 등을 직접 입력해요.")
                    IntroTile("3", "파일명과 저장 위치 자동 구성",
                        "예: DZlog_20261013_001.jpg\nPictures/DZlog/")
                    Text("이 단계는 나중에 사용법에서 다시 볼 수 있어요.",
                        color = DDZColor.TextSecondary)
                }
                else -> {
                    Text("어떤 기록을 시작할까요?",
                        color = DDZColor.TextPrimary, fontWeight = FontWeight.Bold,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    Text("나중에도 양식을 바꿀 수 있어요.", color = DDZColor.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { item ->
                            val active = item.id == category
                            Box(
                                Modifier.weight(1f)
                                    .border(1.dp, if (active) DDZColor.Selected else DDZColor.Border,
                                        RoundedCornerShape(DDZLayout.Radius.Medium))
                                    .background(if (active) DDZColor.SelectedSoft else DDZColor.Surface,
                                        RoundedCornerShape(DDZLayout.Radius.Medium))
                                    .clickable {
                                        category = item.id
                                        chosen = OnboardingStarterCatalog.definitions.first { it.category == item.id }.id
                                    }
                                    .padding(vertical = 14.dp, horizontal = 3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(item.title, color = DDZColor.TextPrimary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                    maxLines = 1)
                            }
                        }
                    }
                    if (category != "basic") {
                        Text("다른 양식 살펴보기", color = DDZColor.TextSecondary,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OnboardingStarterCatalog.definitions.filter { it.category == category }
                                .forEach { def ->
                                    val active = chosen == def.id
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .background(
                                                if (active) DDZColor.SelectedSoft else DDZColor.Surface,
                                                RoundedCornerShape(DDZLayout.Radius.Small))
                                            .clickable { chosen = def.id }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                    ) {
                                        Text(def.title, color = DDZColor.TextPrimary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                        }
                    }
                    StarterPreview(selected)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Column(
            Modifier.fillMaxWidth().background(DDZColor.Surface).padding(
                horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DDZButton(
                text = if (step == 3) {
                    if (readOnly) "도움말 닫기" else "이 양식으로 시작"
                } else if (step == 1) "시작하기" else "다음",
                onClick = {
                    if (step == 3) {
                        if (readOnly) onClose() else onChoose(chosen)
                    } else step++
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy, minHeight = DDZLayout.Control.Button,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DDZButton(
                    text = if (step == 1) {
                        if (readOnly) "닫기" else "건너뛰기"
                    } else "이전",
                    onClick = { if (step == 1) {
                        if (readOnly) onClose() else onSkip()
                    } else step-- },
                    enabled = !busy, style = DDZButtonStyle.Text,
                )
                if (step != 1 && !readOnly) {
                    DDZButton(text = "건너뛰기", onClick = onSkip,
                        enabled = !busy, style = DDZButtonStyle.Text)
                } else Spacer(Modifier.height(1.dp))
            }
            if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun IntroTile(number: String, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth()
        .background(DDZColor.Surface, RoundedCornerShape(DDZLayout.Radius.Medium))
        .padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(number, color = DDZColor.Selected, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = DDZColor.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = DDZColor.TextSecondary)
        }
    }
}

@Composable
private fun StarterPreview(def: OnboardingStarterDefinition) {
    val state = remember(def.id) { requireNotNull(OnboardingStarterCatalog.buildState(def.id)) }
    val now = remember(def.id) { Date() }
    val resolved = remember(state, now) {
        TableResolver().plan(
            cells = state.cells, captureNow = now,
            config = TableResolver.Config(3, "yyyyMMdd", "HHmm")
        ).resolvedCells
    }
    val fileName = remember(state, resolved) {
        buildFileName(resolved, state.fileNameSlotDrafts, "_", 3, 1, now,
            "yyyyMMdd", "HHmm")
    }
    val savePath = remember(state, resolved) {
        buildSavePath(resolved, state.pathSlotDrafts, now, "yyyyMMdd", "HHmm")
    }
    Column(
        Modifier.fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(DDZLayout.Radius.Large))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(def.title, color = DDZColor.TextPrimary, fontWeight = FontWeight.Bold)
        Box(
            Modifier.fillMaxWidth().height(155.dp)
                .background(DDZColor.AmbientWarm, RoundedCornerShape(DDZLayout.Radius.Medium))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.fillMaxWidth()
                .background(DDZColor.Surface, RoundedCornerShape(DDZLayout.Radius.Small))
                .border(1.dp, DDZColor.Border, RoundedCornerShape(DDZLayout.Radius.Small)),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    def.labels.forEach { title ->
                        Box(Modifier.weight(1f).padding(5.dp), contentAlignment = Alignment.Center) {
                            Text(title, color = DDZColor.TextPrimary,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().background(DDZColor.Background)) {
                    state.cells.filter { it.rowIndex == 1 }.forEach { cell ->
                        Box(Modifier.weight(1f).padding(7.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (cell.dataType == TableCellDataType.DATE) "자동 날짜"
                                else if (cell.dataType == TableCellDataType.COUNTER) "001"
                                else "입력",
                                color = DDZColor.TextSecondary,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        Text("첫 촬영 기준 · 입력값은 빈칸으로 시작해요.",
            color = DDZColor.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        Text("파일명", color = DDZColor.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        Text(fileName, color = DDZColor.TextPrimary, fontWeight = FontWeight.Medium)
        Text("저장 위치", color = DDZColor.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        Text(savePath, color = DDZColor.TextPrimary)
    }
}
