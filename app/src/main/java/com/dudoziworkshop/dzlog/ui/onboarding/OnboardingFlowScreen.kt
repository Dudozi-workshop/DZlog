package com.dudoziworkshop.dzlog.ui.onboarding

import android.app.Activity
import androidx.core.view.WindowInsetsControllerCompat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

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
    BackHandler(enabled = readOnly || step == 2) {
        if (step > 1) step-- else onClose()
    }
    val activity = LocalContext.current as? Activity
    val view = LocalView.current
    // Both introduction pages have light backgrounds and need dark status-bar icons.
    // Keep the approved page-one appearance; restore the caller's setting on exit.
    DisposableEffect(step, activity, view) {
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowInsetsControllerCompat(window, view)
            val original = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = true
            onDispose { controller.isAppearanceLightStatusBars = original }
        }
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
            introStyle = step <= 2,
        )
        BoxWithConstraints(Modifier.weight(1f)) {
            val compact = maxHeight < 600.dp
            // The intro caption is fixed above the CTA; reserve space for it while
            // giving the photo the room freed by the removed two-line subtitle.
            // Larger accessibility text may scroll inside the intro content.
            val introHeroHeight = (maxHeight - if (compact) 125.dp else 145.dp)
                .coerceIn(if (compact) 205.dp else 270.dp, if (compact) 420.dp else 530.dp)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = if (step == 1) 0.dp else 20.dp),
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
                else -> OnboardingStarterPage(
                    category = category,
                    selected = selected,
                    compact = compact,
                    onCategory = { id ->
                        category = id
                        chosen = OnboardingStarterCatalog.definitions
                            .first { it.category == id }.id
                        showAlternatives = false
                    },
                    onBrowse = { showAlternatives = true },
                )
            }
            Spacer(Modifier.height(if (compact) 0.dp else 5.dp))
            }
        }
        Column(
            Modifier.fillMaxWidth().background(if (step == 2) DDZColor.Surface else DDZColor.Background).padding(
                horizontal = 20.dp, vertical = if (step == 1) 11.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (step == 1) 10.dp else 6.dp),
        ) {
            if (step == 1) {
                // Keep the decorative page-one tagline on one line. At large
                // system font sizes, its display size stays at the normal
                // 14sp equivalent; actionable UI and other text still scale.
                val captionFontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
                Text(
                    text = "촬영은 간편하게, 기록은 선명하게.",
                    style = DDZTypography.Body.copy(
                        fontSize = (14f / captionFontScale).sp,
                        lineHeight = (20f / captionFontScale).sp,
                    ),
                    color = DDZColor.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DDZButton(
                text = when {
                    step == 1 -> if (readOnly) "다음" else "시작하기"
                    introOnly && step == 2 -> "사용 준비하기"
                    introOnly -> "다음"
                    step == 3 -> if (readOnly) "도움말 닫기" else "DZlog 시작하기"
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
                enabled = !busy, minHeight = 56.dp,
                shape = RoundedCornerShape(15.dp),
                textStyleOverride = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                ),
            )
            // Page four has one clear action; Settings replay retains a close escape.
            if (step == 1 && readOnly) {
                DDZButton(
                    text = "닫기", onClick = onClose,
                    enabled = !busy, style = DDZButtonStyle.Text,
                )
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

