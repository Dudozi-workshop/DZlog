package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CameraNextCaptureInfoPopup(
    nextFileName: String,
    relativePath: String,
    onOpenSaveSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(DDZColor.PrimaryDark.copy(alpha = 0.20f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 20.dp, end = 20.dp, top = 50.dp)
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(16.dp),
            color = DDZColor.Card,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("다음 촬영 정보", style = DDZTypography.OverlayTitleCompact, color = DDZColor.Primary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
                Text("파일명", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Surface(
                    color = DDZColor.Surface,
                    border = BorderStroke(1.dp, DDZColor.Border),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        nextFileName.removeSuffix(".jpg").removeSuffix(".jpeg"),
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        style = DDZTypography.Body,
                        color = DDZColor.TextStrong,
                    )
                }
                Text("저장 경로", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                Surface(
                    color = DDZColor.Surface,
                    border = BorderStroke(1.dp, DDZColor.Border),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        relativePath.ifBlank { "기본 저장 경로" },
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        style = DDZTypography.Body,
                        color = DDZColor.TextStrong,
                    )
                }
                Button(onClick = onOpenSaveSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("저장 설정으로 이동")
                }
            }
        }
    }
}
