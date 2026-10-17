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
enum class FirstCaptureGuidePhase {
    QUICK_EDIT, ZOOM_FOCUS, ZOOM_CHIP, TOOLS, SHUTTER, SAVED, SETTINGS, UNDO, DISMISSED,
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
        FirstCaptureGuidePhase.ZOOM_FOCUS, FirstCaptureGuidePhase.ZOOM_CHIP -> 2
        FirstCaptureGuidePhase.TOOLS -> 3
        FirstCaptureGuidePhase.SHUTTER -> 4
        FirstCaptureGuidePhase.SAVED -> 5
        FirstCaptureGuidePhase.SETTINGS, FirstCaptureGuidePhase.UNDO, FirstCaptureGuidePhase.DISMISSED -> 5
    }
    val title = when (phase) {
        FirstCaptureGuidePhase.QUICK_EDIT -> "빠른 수정"
        FirstCaptureGuidePhase.ZOOM_FOCUS, FirstCaptureGuidePhase.ZOOM_CHIP -> "배율과 초점"
        FirstCaptureGuidePhase.TOOLS -> "촬영 보조도구"
        FirstCaptureGuidePhase.SHUTTER -> "촬영 버튼"
        FirstCaptureGuidePhase.SAVED -> "저장 결과 확인"
        FirstCaptureGuidePhase.SETTINGS -> "설정"
        FirstCaptureGuidePhase.UNDO -> "Undo"
        FirstCaptureGuidePhase.DISMISSED -> ""
    }
    val description = when (phase) {
        FirstCaptureGuidePhase.QUICK_EDIT ->
            "오른쪽 아래 빠른 수정에서는 표에 표시할 기록값을 촬영 전에 확인하고 변경할 수 있어요. 지금 입력할 필요는 없어요."
        FirstCaptureGuidePhase.ZOOM_FOCUS, FirstCaptureGuidePhase.ZOOM_CHIP ->
            "카메라 화면에서 두 손가락을 벌리거나 오므리면 배율을 조절할 수 있어요. 배율 칩과 초점 조절 기능도 사용할 수 있어요. 여기서는 조작하지 않아도 돼요."
        FirstCaptureGuidePhase.TOOLS ->
            "아래 도구 버튼에는 플래시, 초점, 격자, 표 표시처럼 촬영에 도움이 되는 기능이 있어요. 기능마다 필요한 상황에 선택해 사용하면 돼요."
        FirstCaptureGuidePhase.SHUTTER ->
            "가운데 촬영 버튼을 누르면 사진을 찍고 설정된 저장 방식으로 저장해요. 이 안내에서는 촬영하지 않아도 돼요."
        FirstCaptureGuidePhase.SAVED ->
            "촬영한 사진은 최근 촬영과 앨범에서 확인할 수 있어요. 파일명과 저장 위치는 저장설정에서 관리해요."
        FirstCaptureGuidePhase.SETTINGS -> "카메라 상단 설정에서 촬영과 미리보기 옵션을 확인할 수 있어요."
        FirstCaptureGuidePhase.UNDO -> "하단 Undo로 직전 촬영의 취소 기능을 확인할 수 있어요. 안내 중에는 누를 필요가 없어요."
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
                    text = "카메라 기능 알아보기 · $index/5",
                    style = DDZTypography.SettingLabel,
                    color = DDZColor.PrimaryDark,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSkip) {
                    Text("건너뛰기", color = DDZColor.TextSecondary)
                }
            }
            LinearProgressIndicator(
                progress = { index / 5f },
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
                    Text(if (index == 5) "마치기" else "다음")
                }
            }
            if (index == 5) {
                TextButton(onClick = onOpenHelp) {
                    Text("상세 사용법 알아보기", color = DDZColor.PrimaryDark)
                }
            }
        }
    }
}
