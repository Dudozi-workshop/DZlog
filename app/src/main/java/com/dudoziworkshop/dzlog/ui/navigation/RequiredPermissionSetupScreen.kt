package com.dudoziworkshop.dzlog.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
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
import com.dudoziworkshop.dzlog.ui.onboarding.OnboardingTextStyles
import com.dudoziworkshop.dzlog.ui.onboarding.OnboardingProgressHeader
import com.dudoziworkshop.dzlog.ui.onboarding.OnboardingPermissionArtwork
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
    // Light onboarding background needs dark system icons. The previous mode
    // must be restored when the permission screen leaves the composition.
    val permissionActivity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val permissionView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(onboarding, permissionActivity, permissionView) {
        val window = permissionActivity?.window
        if (!onboarding || window == null) {
            onDispose { }
        } else {
            val controller = androidx.core.view.WindowInsetsControllerCompat(window, permissionView)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = true
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }
    val actionLabel = when {
        working -> "확인 중…"
        currentStep == RequiredSetupStep.CAMERA && cameraRequestAttempted -> "휴대폰 권한 설정 열기"
        currentStep == RequiredSetupStep.PHOTOS &&
            (photoRequestAttempted || state.photos == GalleryMediaAccess.SELECTED_ONLY) ->
            "휴대폰 권한 설정 열기"
        currentStep == RequiredSetupStep.CAMERA -> "카메라 권한 허용하기"
        currentStep == RequiredSetupStep.PHOTOS -> "사진 접근 허용하기"
        onboarding -> "Pictures 폴더 선택하기"
        else -> "DZlog 폴더 연결"
    }

    Column(
        Modifier.fillMaxSize().background(colors.Background)
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        if (onboarding) {
            OnboardingProgressHeader(stage = 3, label = "권한 설정")
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text("DZlog", style = typography.ScreenTitle, color = colors.PrimaryDark)
                Text("권한 복구", style = typography.Caption, color = colors.TextSecondary)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (onboarding) 10.dp else 14.dp),
        ) {
            Text(
                if (onboarding) "필요한 권한을\n설정해 주세요." else "필요한 권한을 확인해 주세요",
                style = if (onboarding) typography.ScreenTitle.copy(
                    fontSize = OnboardingTextStyles.PageTitle.fontSize,
                    lineHeight = OnboardingTextStyles.PageTitle.lineHeight,
                    fontWeight = FontWeight.Bold,
                ) else typography.ScreenTitle,
                color = colors.TextPrimary,
            )
            if (onboarding) {
                OnboardingPermissionOverview(state, folderConnected, currentStep, working, onAction)
            } else {
                Text(
                    "사용 중 해제된 권한이 있다면 다시 설정해 주세요. 기록은 그대로 보존돼요.",
                    style = typography.Body, color = colors.TextSecondary,
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
            }
            // Display the purpose and next action before the permission dialog.
            CurrentSetupGuide(
                currentStep,
                state.photos,
                cameraRequestAttempted,
                photoRequestAttempted,
                onboarding = onboarding,
            )
            if (onboarding) OnboardingPermissionArtwork()
            if (!notice.isNullOrBlank()) {
                Text(notice, style = typography.Body, color = colors.Destructive)
            }
            if (!onboarding) {
                Text(
                    "이미 허용된 항목은 다시 요청하지 않아요.",
                    style = typography.Caption,
                    color = colors.TextSecondary,
                )
            }
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
                shape = if (onboarding) androidx.compose.foundation.shape.RoundedCornerShape(16.dp) else null,
            )
        }
    }
}

/** Onboarding-only decoration. Grant state and permission requests remain in RequiredPermissionsGate. */
@Composable
private fun OnboardingPermissionOverview(
    state: RequiredPermissionState,
    folderConnected: Boolean,
    currentStep: RequiredSetupStep,
    working: Boolean,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OnboardingPermissionCard(
            "카메라", "사진 촬영과 기록표 촬영에 필요해요.",
            Icons.Default.CameraAlt, Color(0xFF91A77E),
            if (state.camera) "허용됨" else "허용 필요", state.camera,
        )
        OnboardingPermissionCard(
            "사진 접근", "저장된 사진을 갤러리에서 확인해요.",
            Icons.Default.Image, Color(0xFFB69A83),
            when (state.photos) {
                GalleryMediaAccess.FULL -> "허용됨"
                GalleryMediaAccess.SELECTED_ONLY -> "일부 허용"
                GalleryMediaAccess.NOT_GRANTED -> "허용 필요"
            }, state.photos == GalleryMediaAccess.FULL,
        )
        OnboardingPermissionCard(
            "저장 폴더", "Pictures를 선택하면 DZlog 폴더를 자동으로 준비해요.",
            Icons.Default.Folder, Color(0xFF91A77E),
            if (folderConnected) "연결됨" else "연결 필요", folderConnected,
            if (!folderConnected && currentStep == RequiredSetupStep.FOLDER) onAction else null,
            working,
        )
    }
}

@Composable
private fun OnboardingPermissionCard(
    title: String, detail: String, icon: ImageVector, iconColor: Color,
    status: String, granted: Boolean,
    folderAction: (() -> Unit)? = null, working: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF5F2E9),
        shape = RoundedCornerShape(15.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(
                    Modifier.size(34.dp).background(iconColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White,
                        modifier = Modifier.size(20.dp))
                }
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    color = DDZColor.TextPrimary,
                    style = OnboardingTextStyles.CardTitle,
                    maxLines = 2,
                )
                if (folderAction != null) {
                    Surface(
                        modifier = Modifier.clickable(enabled = !working) { folderAction() },
                        color = Color(0xFFFEFCF8),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFAB8A70)),
                    ) {
                        Text(
                            "연결하기",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            fontSize = 12.sp,
                            color = DDZColor.PrimaryDark,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Surface(
                        color = if (granted) Color(0xFFE1E8D6) else Color(0xFFF0E6DA),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Text(
                            status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            color = if (granted) Color(0xFF49643A) else DDZColor.PrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                detail,
                modifier = Modifier.fillMaxWidth(),
                color = DDZColor.TextSecondary,
                style = OnboardingTextStyles.CardDetail,
            )
        }
    }
}
