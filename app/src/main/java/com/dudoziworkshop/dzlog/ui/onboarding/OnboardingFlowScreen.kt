package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.saveable.rememberSaveable
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

private data class StarterCategory(val id: String, val title: String, val detail: String)
private val categories = listOf(
    StarterCategory("basic", "기본 촬영", "가볍게 사진과 기록을 남겨요"),
    StarterCategory("daily", "일상·취미", "여행 · 반려동물 · 취미 기록"),
    StarterCategory("work", "업무·현장", "현장 · 점검 · 연구 기록"),
)

@Composable
internal fun OnboardingFlowScreen(
    busy: Boolean,
    onChoose: (String) -> Unit,
    onSkip: () -> Unit,
    readOnly: Boolean = false,
    onClose: () -> Unit = {},
    introOnly: Boolean = false,
    onIntroComplete: () -> Unit = {},
    startStep: Int = 1,
) {
    var step by rememberSaveable(introOnly, startStep) {
        mutableIntStateOf(startStep.coerceIn(1, 3))
    }
    BackHandler(enabled = readOnly) {
        if (step > 1) step-- else onClose()
    }
    var category by rememberSaveable { mutableStateOf("basic") }
    var chosen by rememberSaveable { mutableStateOf(OnboardingStarterCatalog.DEFAULT_ID) }
    var showAlternatives by rememberSaveable { mutableStateOf(false) }
    val selected = OnboardingStarterCatalog.find(chosen)
        ?: OnboardingStarterCatalog.definitions.first()

    Column(
        Modifier.fillMaxSize().background(DDZColor.Background).statusBarsPadding().navigationBarsPadding(),
    ) {
        OnboardingProgressHeader(
            stage = when (step) { 1 -> 1; 2 -> 2; else -> 4 },
            label = when (step) { 1 -> "앱 소개"; 2 -> "주요 기능"; else -> "시작 양식" },
        )
        BoxWithConstraints(Modifier.weight(1f)) {
            val compact = maxHeight < 600.dp
            // Keep the approved poster-like hero dominant without sacrificing
            // the bottom caption on ordinary-size screens. Accessibility
            // text scaling may scroll rather than clip any controls.
            val introHeroHeight = (maxHeight - if (compact) 202.dp else 230.dp)
                .coerceIn(if (compact) 192.dp else 260.dp, if (compact) 340.dp else 430.dp)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
            ) {
            when (step) {
                1 -> OnboardingBrandIntro(
                    heroHeight = introHeroHeight,
                    compact = compact,
                )
                2 -> OnboardingFeatureIntro(
                    previewHeight = if (compact) 104.dp else 146.dp,
                    compact = compact,
                )
                else -> {
                    Text("어떤 기록을 시작할까요?",
                        color = DDZColor.TextPrimary, fontWeight = FontWeight.Bold,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                    Text("나에게 맞는 양식을 골라보세요. 나중에도 바꿀 수 있어요.", color = DDZColor.TextSecondary)
                    OnboardingStarterCategories(
                        selectedId = category,
                        onSelect = { id ->
                            category = id
                            chosen = OnboardingStarterCatalog.definitions
                                .first { it.category == id }.id
                            showAlternatives = false
                        },
                        onBrowse = { showAlternatives = true },
                        compact = compact,
                    )
                    StarterPreview(selected, if (compact) 120.dp else 184.dp, compact)
                }
            }
            Spacer(Modifier.height(if (compact) 0.dp else 5.dp))
            }
        }
        Column(
            Modifier.fillMaxWidth().background(DDZColor.Surface).padding(
                horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DDZButton(
                text = when {
                    introOnly && step == 2 -> "사용 준비하기"
                    introOnly -> "다음"
                    step == 3 -> if (readOnly) "도움말 닫기" else "DZlog 시작하기"
                    step == 1 -> "다음"
                    else -> "다음"
                },
                onClick = {
                    when {
                        introOnly && step == 2 -> onIntroComplete()
                        introOnly -> step++
                        step == 3 -> if (readOnly) onClose() else onChoose(chosen)
                        else -> step++
                    }
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
                        if (readOnly) onClose()
                        else if (introOnly) onIntroComplete() else onSkip()
                    } else step-- },
                    enabled = !busy, style = DDZButtonStyle.Text,
                )
                if (step != 1 && !readOnly) {
                    DDZButton(text = "건너뛰기",
                        onClick = if (introOnly) onIntroComplete else onSkip,
                        enabled = !busy, style = DDZButtonStyle.Text)
                } else Spacer(Modifier.height(1.dp))
            }
            if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }
    if (showAlternatives && category != "basic") {
        OnboardingStarterAlternativeDialog(
            category = category,
            selectedId = chosen,
            onSelect = { id ->
                chosen = id
                showAlternatives = false
            },
            onDismiss = { showAlternatives = false },
        )
    }
}

@Composable
private fun StarterPreview(
    def: OnboardingStarterDefinition,
    previewHeight: androidx.compose.ui.unit.Dp,
    compact: Boolean,
) {
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
    var showSaveDetails by rememberSaveable(def.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(DDZLayout.Radius.Large))
            .padding(if (compact) 9.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
    ) {
        Text(def.title, color = DDZColor.TextPrimary, fontWeight = FontWeight.Bold)
        OnboardingPhotoShowcase(
            labels = def.labels,
            values = state.cells.filter { it.rowIndex == 1 }.map { cell ->
                when (cell.dataType) {
                    TableCellDataType.DATE -> "자동 날짜"
                    TableCellDataType.COUNTER -> "001"
                    else -> "입력"
                }
            },
            height = previewHeight,
            compact = true,
            showGalleryStrip = true,
        )
        Text(if (compact) "기록표 숨기기 가능 · 입력값은 빈칸에서 시작"
             else "촬영 중 기록표 숨기기 가능 · 첫 입력값은 빈칸으로 시작",
            color = DDZColor.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
        Row(
            Modifier.fillMaxWidth()
                .clickable { showSaveDetails = !showSaveDetails }
                .padding(vertical = if (compact) 5.dp else 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (showSaveDetails) "저장 규칙 접기" else "저장 규칙 미리보기",
                color = DDZColor.PrimaryDark,
                fontWeight = FontWeight.Medium,
            )
            Text(
                if (showSaveDetails) "⌃" else "⌄",
                color = DDZColor.TextSecondary,
            )
        }
        if (showSaveDetails) {
            Text(
                "파일명", color = DDZColor.TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            )
            Text(
                fileName,
                color = DDZColor.TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "저장 위치", color = DDZColor.TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            )
            Text(
                savePath,
                color = DDZColor.TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
