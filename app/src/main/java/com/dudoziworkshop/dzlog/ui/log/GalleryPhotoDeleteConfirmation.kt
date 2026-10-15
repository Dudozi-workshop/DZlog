package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

/** Photo deletion uses the same dialog system as folder deletion.
 * The caller continues to own the original MediaStore permission flow and retries. */
@Composable
internal fun GalleryPhotoDeleteConfirmation(
    selected: List<MediaImageItem>,
    allPhotos: List<MediaImageItem>,
    busy: Boolean,
    onCancel: () -> Unit,
    onConfirm: (GalleryPhotoDeletePlan) -> Unit,
) {
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    val originals = remember(selected, allPhotos) {
        GalleryPhotoMovePolicy.pairedOriginals(selected, allPhotos)
    }
    var includeOriginals by remember(selected, allPhotos) { mutableStateOf(false) }
    val prepared = remember(selected, allPhotos, includeOriginals) {
        runCatching { GalleryPhotoDeletion.prepare(selected, allPhotos, includeOriginals) }
    }
    val plan = prepared.getOrNull()

    DDZContentDialog(
        title = "선택한 사진을 삭제할까요?",
        onDismiss = { if (!busy) onCancel() },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                Text("삭제할 사진과 원본 포함 여부를 확인해 주세요.",
                    style = typography.Body, color = colors.TextSecondary)
                GalleryFolderInfoCard(
                    name = "선택한 사진",
                    detail = "사진 ${selected.distinctBy { it.id }.size}장 · 연결된 원본 ${originals.size}장",
                )
                if (originals.isNotEmpty()) {
                    DDZButton(
                        text = if (!includeOriginals) "✓ 선택한 사진만 삭제" else "선택한 사진만 삭제",
                        onClick = { includeOriginals = false },
                        style = if (!includeOriginals) DDZButtonStyle.Primary else DDZButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    )
                    DDZButton(
                        text = if (includeOriginals) "✓ 원본 포함 삭제" else "원본 포함 삭제",
                        onClick = { includeOriginals = true },
                        style = if (includeOriginals) DDZButtonStyle.Primary else DDZButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    )
                }
                plan?.let { Text("총 삭제 파일 ${it.items.size}개",
                    style = typography.Body, color = colors.TextPrimary) }
                Surface(color = colors.DestructiveSoft, shape = MaterialTheme.shapes.medium) {
                    Text("삭제한 사진은 되돌릴 수 없습니다.",
                        modifier = Modifier.padding(spacing.controlGap),
                        style = typography.Secondary, color = colors.TextPrimary)
                }
                prepared.exceptionOrNull()?.message?.let {
                    Text(it, color = colors.Destructive, style = typography.Secondary)
                }
            }
        },
        actions = {
            DDZButton(
                text = "취소", onClick = onCancel, modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary, enabled = !busy,
            )
            DDZButton(
                text = "삭제", onClick = { plan?.let(onConfirm) },
                modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Destructive,
                enabled = !busy && plan != null,
            )
        },
    )
}
