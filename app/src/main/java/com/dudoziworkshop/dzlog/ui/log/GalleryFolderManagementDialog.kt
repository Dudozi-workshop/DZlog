package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.log.policy.CapturePathImpact
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderOperationPolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

/** Compact deletion-style confirmation. Only the caller performs any file operation. */
@Composable
internal fun GalleryFolderManagementDialog(
    action: String,
    sourcePath: String,
    renameName: String,
    destinationPath: String,
    onRenameNameChange: (String) -> Unit,
    impact: CapturePathImpact,
    busy: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val rename = action == "rename"
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    val proposed = runCatching {
        if (rename) GalleryFolderOperationPolicy.renamedPath(sourcePath, renameName)
        else GalleryFolderOperationPolicy.movedPath(sourcePath, destinationPath)
    }
    val impactNote = if (impact != CapturePathImpact.NONE) {
        "변경 후 촬영 저장경로를 확인해 주세요."
    } else if (rename) {
        "촬영 저장설정은 변경되지 않습니다."
    } else {
        "사진·원본·하위 폴더도 함께 이동합니다."
    }

    DDZContentDialog(
        title = if (rename) "폴더 이름 변경" else "폴더 이동",
        onDismiss = { if (!busy) onCancel() },
        content = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
            ) {
                GalleryPathPreviewCard("현재 경로", galleryReadablePath(sourcePath))
                if (rename) {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = onRenameNameChange,
                        label = { Text("새 폴더 이름") },
                        enabled = !busy,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    GalleryPathChangeArrow()
                    GalleryPathPreviewCard(
                        "이동 후 경로",
                        proposed.getOrNull()?.let(::galleryReadablePath) ?: "경로 확인 필요",
                    )
                }
                if (rename) {
                    GalleryPathChangeArrow()
                    GalleryPathPreviewCard(
                        "변경 후 경로",
                        proposed.getOrNull()?.let(::galleryReadablePath) ?: "새 이름을 입력해 주세요.",
                    )
                }
                Text(impactNote, style = typography.Caption, color = colors.TextSecondary)
                proposed.exceptionOrNull()?.message?.let {
                    Text(it, style = typography.Caption, color = colors.Destructive)
                }
                error?.let { Text(it, style = typography.Caption, color = colors.Destructive) }
            }
        },
        actions = {
            DDZButton("취소", onClick = onCancel, modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary, enabled = !busy)
            DDZButton(if (rename) "이름 변경" else "이동", onClick = onConfirm,
                modifier = Modifier.weight(1f), enabled = !busy && proposed.isSuccess)
        },
    )
}
