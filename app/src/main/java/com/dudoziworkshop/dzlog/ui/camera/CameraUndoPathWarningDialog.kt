package com.dudoziworkshop.dzlog.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

internal data class PendingCameraUndoPathWarning(
    val captureUris: List<String>,
    val capturedFolder: String,
    val currentFolder: String,
)

/** The calling screen owns deletion, consent validity and its Undo stack. */
@Composable
internal fun CameraUndoPathWarningDialog(
    capturedFolder: String,
    currentFolder: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    val spacing = LocalDDZSpacing.current

    DDZContentDialog(
        title = "다른 폴더의 사진을 삭제할까요?",
        onDismiss = onCancel,
        content = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
            ) {
                Text(
                    "삭제하려는 사진은 현재 촬영 폴더와 다른 위치에 저장되어 있습니다.",
                    style = typography.Body,
                    color = colors.TextSecondary,
                )
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.SurfaceSoft, RoundedCornerShape(12.dp))
                        .padding(spacing.controlGap),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text("삭제할 사진의 저장경로", style = typography.Caption, color = colors.TextSecondary)
                    Text(capturedFolder, style = typography.Secondary, color = colors.TextPrimary)
                    Text(
                        "현재 촬영 저장경로",
                        style = typography.Caption,
                        color = colors.TextSecondary,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                    Text(currentFolder, style = typography.Secondary, color = colors.TextPrimary)
                }
                Text(
                    "사진을 삭제해도 현재 저장 설정은 유지됩니다.",
                    style = typography.Secondary,
                    color = colors.TextSecondary,
                )
            }
        },
        actions = {
            DDZButton(
                text = "취소",
                onClick = onCancel,
                style = DDZButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
            DDZButton(
                text = "사진 삭제",
                onClick = onConfirm,
                style = DDZButtonStyle.Primary,
                containerColorOverride = colors.Destructive,
                modifier = Modifier.weight(1f),
            )
        },
    )
}
