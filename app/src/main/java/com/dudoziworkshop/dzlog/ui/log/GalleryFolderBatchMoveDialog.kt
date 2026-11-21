package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderBatchPolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@Composable
internal fun GalleryFolderBatchMoveDialog(
    sources: List<String>,
    destination: String,
    knownFolders: List<String>,
    capturePathWarning: Boolean,
    busy: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    val planned = runCatching { GalleryFolderBatchPolicy.moveTargets(sources, destination, knownFolders) }
    DDZContentDialog(
        title = "폴더 ${sources.size}개 이동",
        onDismiss = { if (!busy) onCancel() },
        content = {
            Column(Modifier.heightIn(max = 330.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                GalleryPathPreviewCard("현재 폴더", sources.take(4).joinToString("\n", transform = ::galleryReadablePath) +
                    if (sources.size > 4) "\n외 ${sources.size - 4}개" else "")
                GalleryPathChangeArrow()
                GalleryPathPreviewCard("이동할 위치", galleryReadablePath(destination))
                Text("선택한 폴더의 사진·원본·하위 폴더도 함께 이동합니다.",
                    style = typography.Caption, color = colors.TextSecondary)
                if (capturePathWarning) Text("이동 후 촬영 저장경로를 다시 확인해 주세요.",
                    style = typography.Caption, color = colors.Destructive)
                planned.exceptionOrNull()?.message?.let {
                    Text(it, style = typography.Caption, color = colors.Destructive)
                }
            }
        },
        actions = {
            DDZButton("취소", onClick = onCancel, modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary, enabled = !busy)
            DDZButton("이동", onClick = onConfirm, modifier = Modifier.weight(1f),
                enabled = !busy && planned.isSuccess)
        },
    )
}
