package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

/** Preserve original-photo selection and final approval in the same deletion-style dialog. */
@Composable
internal fun GalleryPhotoMoveConfirmation(
    selected: List<MediaImageItem>,
    allPhotos: List<MediaImageItem>,
    destinationPath: String,
    busy: Boolean,
    onCancel: () -> Unit,
    onConfirm: (GalleryPhotoMovePlan) -> Unit,
) {
    val originals = remember(selected, allPhotos) {
        GalleryPhotoMovePolicy.pairedOriginals(selected, allPhotos)
    }
    var includeOriginals by remember(selected, destinationPath) { mutableStateOf<Boolean?>(null) }
    var finalStep by remember(selected, destinationPath) { mutableStateOf(false) }
    val showFinal = originals.isEmpty() || finalStep
    val planned = remember(selected, originals, destinationPath, includeOriginals) {
        runCatching {
            GalleryPhotoMovePolicy.plan(selected, originals, destinationPath, includeOriginals == true)
        }
    }
    val plan = planned.getOrNull()
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    DDZContentDialog(
        title = if (showFinal) "사진 이동" else "원본사진 포함",
        onDismiss = { if (!busy) onCancel() },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                val sources = selected.map { galleryReadablePath(it.relativePath) }.distinct()
                val currentPathLabel = if (sources.size == 1) sources.single()
                    else "${sources.size}개 폴더에서 선택"
                GalleryPathPreviewCard("현재 경로", currentPathLabel)
                GalleryPathChangeArrow()
                GalleryPathPreviewCard("이동 후 경로", galleryReadablePath(destinationPath))
                if (sources.size > 1) {
                    Text("선택한 사진 ${selected.size}장", style = typography.Caption,
                        color = colors.TextSecondary)
                }
                if (!showFinal) {
                    Text("원본사진 ${originals.size}장도 함께 이동할까요?",
                        style = typography.Body, color = colors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                        DDZButton(
                            "사진만", onClick = { includeOriginals = false },
                            modifier = Modifier.weight(1f),
                            style = if (includeOriginals == false) DDZButtonStyle.Primary else DDZButtonStyle.Secondary,
                            enabled = !busy,
                        )
                        DDZButton(
                            "원본 포함", onClick = { includeOriginals = true },
                            modifier = Modifier.weight(1f),
                            style = if (includeOriginals == true) DDZButtonStyle.Primary else DDZButtonStyle.Secondary,
                            enabled = !busy,
                        )
                    }
                } else if (plan != null) {
                    val originalCount = plan.items.count {
                        GalleryPhotoMovePolicy.isOriginalPath(it.relativePath)
                    }
                    Text("결과사진 ${plan.items.size - originalCount}장 · 원본사진 ${originalCount}장",
                        style = typography.Body, color = colors.TextPrimary)
                    Text("촬영 저장설정은 변경되지 않습니다.",
                        style = typography.Caption, color = colors.TextSecondary)
                    if (originals.isNotEmpty()) {
                        TextButton(enabled = !busy, onClick = { finalStep = false }) {
                            Text("원본 포함 여부 변경")
                        }
                    }
                }
                planned.exceptionOrNull()?.message?.let {
                    Text(it, color = colors.Destructive, style = typography.Caption)
                }
            }
        },
        actions = {
            DDZButton("취소", onClick = onCancel, modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary, enabled = !busy)
            DDZButton(
                if (showFinal) "이동" else "다음",
                onClick = {
                    if (showFinal) onConfirm(requireNotNull(plan)) else finalStep = true
                },
                modifier = Modifier.weight(1f),
                enabled = !busy && plan != null && (showFinal || includeOriginals != null),
            )
        },
    )
}
