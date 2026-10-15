package com.dudoziworkshop.dzlog.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryMediaAccess
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

/**
 * In-app fullscreen setup: no extra app-owned permission popup.
 * Only Android's permission prompt and the system folder picker appear on request.
 * PermissionStatusCards and CurrentSetupGuide are shared with the existing gate.
 */
@Composable
internal fun RequiredPermissionSetupScreen(
    onboarding: Boolean,
    state: RequiredPermissionState,
    folderConnected: Boolean,
    currentStep: RequiredSetupStep,
    cameraRequestAttempted: Boolean,
    photoRequestAttempted: Boolean,
    notice: String?,
    working: Boolean,
    onAction: () -> Unit,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    val actionLabel = when {
        working -> "확인 중…"
        currentStep == RequiredSetupStep.CAMERA && cameraRequestAttempted -> "휴대폰 권한 설정 열기"
        currentStep == RequiredSetupStep.PHOTOS &&
            (photoRequestAttempted || state.photos == GalleryMediaAccess.SELECTED_ONLY) ->
            "휴대폰 권한 설정 열기"
        currentStep == RequiredSetupStep.CAMERA -> "카메라 권한 설정"
        currentStep == RequiredSetupStep.PHOTOS -> "사진 접근 권한 설정"
        else -> "DZlog 폴더 연결"
    }

    Column(
        Modifier.fillMaxSize().background(colors.Background)
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DZlog", style = typography.ScreenTitle, color = colors.PrimaryDark)
            Text(
                if (onboarding) "03 / 04 · 사용 준비" else "권한 복구",
                style = typography.Caption,
                color = colors.TextSecondary,
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (onboarding) "첫 기록을 위한 준비" else "필요한 권한을 확인해 주세요",
                style = typography.ScreenTitle,
                color = colors.TextPrimary,
            )
            Text(
                if (onboarding)
                    "촬영과 사진 관리를 위해 필요한 권한을 설정해 주세요."
                else
                    "사용 중 해제된 권한이 있다면 다시 설정해 주세요. 기록은 그대로 보존돼요.",
                style = typography.Body,
                color = colors.TextSecondary,
            )
            PermissionStatusCards(state, currentStep)
            PermissionCard(
                "저장 폴더 연결",
                if (folderConnected) "연결됨" else "연결 필요",
                if (folderConnected) "Pictures/DZlog 폴더가 연결되어 있어요."
                else "사진과 하위 폴더를 관리하려면 Pictures/DZlog 폴더를 연결해 주세요.",
                active = currentStep == RequiredSetupStep.FOLDER,
                stepNumber = 3,
            )
            CurrentSetupGuide(
                currentStep,
                state.photos,
                cameraRequestAttempted,
                photoRequestAttempted,
            )
            if (!notice.isNullOrBlank()) {
                Text(notice, style = typography.Body, color = colors.Destructive)
            }
            Text(
                "이미 허용된 항목은 다시 요청하지 않아요.",
                style = typography.Caption,
                color = colors.TextSecondary,
            )
        }
        Column(
            Modifier.fillMaxWidth().background(colors.Surface)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            DDZButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                enabled = !working,
            )
        }
    }
}
