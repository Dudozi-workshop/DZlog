package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** The first camera walk-through never intercepts the shutter or changes saved data. */
internal enum class FirstCaptureGuidePhase {
    QUICK_EDIT, SHUTTER, SAVED, DISMISSED,
}

internal data class FirstCaptureSavedResult(
    val displayName: String,
    val relativePath: String,
)

@Composable
internal fun FirstCaptureGuide(
    phase: FirstCaptureGuidePhase,
    result: FirstCaptureSavedResult?,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (phase == FirstCaptureGuidePhase.DISMISSED) return
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = DDZSpacing.screenPadding),
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = when (phase) {
                        FirstCaptureGuidePhase.QUICK_EDIT -> "첫 촬영 · 1/2"
                        FirstCaptureGuidePhase.SHUTTER -> "첫 촬영 · 2/2"
                        FirstCaptureGuidePhase.SAVED -> "첫 사진 저장 완료"
                        FirstCaptureGuidePhase.DISMISSED -> ""
                    },
                    style = DDZTypography.SettingLabel,
                    color = DDZColor.PrimaryDark,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSkip) {
                    Text("안내 닫기", color = DDZColor.TextSecondary)
                }
            }
            Text(
                when (phase) {
                    FirstCaptureGuidePhase.QUICK_EDIT ->
                        "아래 빠른 수정을 눌러 기록명 등 입력할 값을 수정해 보세요. 입력하지 않아도 촬영할 수 있어요."
                    FirstCaptureGuidePhase.SHUTTER ->
                        "준비되면 셔터를 눌러 촬영하세요. 저장이 성공한 뒤 결과를 알려드려요."
                    FirstCaptureGuidePhase.SAVED -> "실제 촬영한 사진이 저장됐어요."
                    FirstCaptureGuidePhase.DISMISSED -> ""
                },
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
            )
            if (phase == FirstCaptureGuidePhase.SAVED && result != null) {
                Text("파일명: " + result.displayName,
                    style = DDZTypography.Body, color = DDZColor.TextPrimary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("저장 위치: " + result.relativePath,
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenGallery, modifier = Modifier.weight(1f)) {
                        Text("갤러리에서 보기")
                    }
                    Button(onClick = onContinue, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark)) {
                        Text("계속 촬영")
                    }
                }
                TextButton(onClick = onOpenHelp) {
                    Text("상세 사용법 알아보기", color = DDZColor.PrimaryDark)
                }
            } else if (phase == FirstCaptureGuidePhase.QUICK_EDIT) {
                TextButton(onClick = onContinue) {
                    Text("빠른 수정 없이 촬영하기", color = DDZColor.PrimaryDark)
                }
            }
        }
    }
}
