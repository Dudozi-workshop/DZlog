package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.common.CounterAwareFileNameText
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

// 2단계 라운딩 토큰: 카메라 상/하단의 자주 노출되는 소형 컨트롤은 Small로 통일한다.
private val CameraCompactControlShape = RoundedCornerShape(DDZLayout.Radius.Small)

@Composable
fun CameraTopBar(
    topDisplayName: String,
    onOpenCaptureInfo: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .padding(
                start = DDZSpacing.screenPadding,
                end = DDZSpacing.screenPadding
            )
            .fillMaxWidth()
    ) {
        val topBarMinHeight = 28.dp + DDZSpacing.itemGap
        // 토큰 정책: 반복되는 32dp 터치 영역은 DDZLayout.Icon.Touch로 고정한다.
        val settingsButtonReservedWidth = DDZLayout.Icon.Touch + (DDZSpacing.cardPadding * 2)
        val filenameMaxWidth = (this@BoxWithConstraints.maxWidth - settingsButtonReservedWidth - DDZSpacing.itemGap)
            .coerceAtLeast(0.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = topBarMinHeight)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .widthIn(max = filenameMaxWidth)
                    .defaultMinSize(minHeight = 30.dp)
                    .background(
                        color = DDZColor.Card.copy(alpha = 0.5f),
                        shape = CameraCompactControlShape
                    )
                    .border(1.dp, DDZColor.SageBorder, CameraCompactControlShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onOpenCaptureInfo() }
                    .padding(horizontal = DDZSpacing.cardPadding, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                CounterAwareFileNameText(
                    fileName = topDisplayName,
                    style = DDZTypography.Caption.copy(lineHeight = 14.sp),
                    color = DDZColor.TextStrong,
                    counterColor = DDZColor.SageDarkStrong,
                    useExactName = true,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(DDZColor.PrimaryDark.copy(alpha = 0f))
                    .defaultMinSize(minWidth = DDZLayout.Icon.Touch, minHeight = DDZLayout.Icon.Touch)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = DDZSpacing.cardPadding, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "촬영 설정",
                    tint = DDZColor.SageDarkStrong
                )
            }
        }
    }
}
