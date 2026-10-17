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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Informational tour: navigation never requires editing, shooting, or saving. */
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
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (phase == FirstCaptureGuidePhase.DISMISSED) return
    val index = when (phase) {
        FirstCaptureGuidePhase.QUICK_EDIT -> 1
        FirstCaptureGuidePhase.SHUTTER -> 2
        FirstCaptureGuidePhase.SAVED -> 3
        FirstCaptureGuidePhase.DISMISSED -> 3
    }
    val title = when (phase) {
        FirstCaptureGuidePhase.QUICK_EDIT -> "빠른 수정"
        FirstCaptureGuidePhase.SHUTTER -> "촬영 버튼"
        FirstCaptureGuidePhase.SAVED -> "저장 결과 확인"
        FirstCaptureGuidePhase.DISMISSED -> ""
    }
    val description = when (phase) {
        FirstCaptureGuidePhase.QUICK_EDIT ->
            "오른쪽 아래 빠른 수정에서는 표에 표시할 기록값을 촬영 전에 확인하고 변경할 수 있어요. 지금 입력할 필요는 없어요."
        FirstCaptureGuidePhase.SHUTTER ->
            "가운데 촬영 버튼을 누르면 사진을 찍고 설정된 저장 방식으로 저장해요. 이 안내에서는 촬영하지 않아도 돼요."
        FirstCaptureGuidePhase.SAVED ->
            "촬영한 사진은 최근 촬영과 앨범에서 확인할 수 있어요. 파일명과 저장 위치는 저장설정에서 관리해요."
        FirstCaptureGuidePhase.DISMISSED -> ""
    }
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
                    text = "카메라 기능 알아보기 · $index/3",
                    style = DDZTypography.SettingLabel,
                    color = DDZColor.PrimaryDark,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSkip) {
                    Text("건너뛰기", color = DDZColor.TextSecondary)
                }
            }
            LinearProgressIndicator(
                progress = { index / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = DDZColor.PrimaryDark,
            )
            Text(title, style = DDZTypography.SettingLabel, color = DDZColor.PrimaryDark)
            Text(description, style = DDZTypography.Body, color = DDZColor.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (index > 1) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("이전")
                    }
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
                ) {
                    Text(if (index == 3) "마치기" else "다음")
                }
            }
            if (index == 3) {
                TextButton(onClick = onOpenHelp) {
                    Text("상세 사용법 알아보기", color = DDZColor.PrimaryDark)
                }
            }
        }
    }
}
