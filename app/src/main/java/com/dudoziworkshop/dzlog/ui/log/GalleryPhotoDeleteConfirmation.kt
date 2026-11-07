package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

/**
 * Single photo-deletion dialog. Lookup, confirmation, Android deletion and
 * failed-file retry all reuse the same modern surface, not separate legacy
 * loading/confirmation dialogs. Permission requests remain OS-owned.
 */
@Composable
internal fun GalleryPhotoDeleteConfirmation(
    selected: List<MediaImageItem>,
    allPhotos: List<MediaImageItem>,
    busy: Boolean,
    allowIncludeOriginals: Boolean = true,
    onCancel: () -> Unit,
    onConfirm: (GalleryPhotoDeletePlan) -> Unit,
    loading: Boolean = false,
    retryPlan: GalleryPhotoDeletePlan? = null,
    errorMessage: String? = null,
    progressText: String? = null,
    onRetry: () -> Unit = {},
) {
    val colors = LocalDDZColor.current
    val spacing = LocalDDZSpacing.current
    val typography = LocalDDZTypography.current
    val originals = remember(selected, allPhotos) {
        GalleryPhotoMovePolicy.pairedOriginals(selected, allPhotos)
    }
    var includeOriginals by remember(selected, allPhotos) { mutableStateOf(false) }
    val readOnlyTour = LocalTutorialReadOnly.current
    val showConfirmation = !busy && retryPlan == null && errorMessage == null
    val prepared = remember(selected, allPhotos, includeOriginals, readOnlyTour, showConfirmation) {
        if (!showConfirmation) null else runCatching {
            if (readOnlyTour) {
                // Tutorial media URIs are not valid MediaStore deletion targets.
                val included = if (includeOriginals) GalleryPhotoMovePolicy.pairedOriginals(selected, allPhotos) else emptyList()
                GalleryPhotoDeletePlan((selected + included).distinctBy { it.id })
            } else GalleryPhotoDeletion.prepare(selected, allPhotos, includeOriginals)
        }
    }
    val plan = prepared?.getOrNull()

    DDZContentDialog(
        title = when {
            retryPlan != null && !busy -> "미완료 파일 삭제 확인"
            errorMessage != null && !busy -> "사진 삭제 확인 실패"
            else -> "선택한 사진을 삭제할까요?"
        },
        onDismiss = { if (!busy) onCancel() },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.controlGap)) {
                when {
                    busy -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.controlGap),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            Text(
                                if (loading) "삭제 대상 확인 중…" else progressText ?: "Android 확인 및 삭제 결과 확인 중…",
                                style = typography.Body,
                                color = colors.TextSecondary,
                            )
                        }
                    }
                    retryPlan != null -> {
                        Text("삭제되지 않은 파일만 다시 요청합니다.",
                            style = typography.Body, color = colors.TextSecondary)
                        GalleryFolderInfoCard(
                            name = "남은 삭제 대상",
                            detail = "결과사진 ${retryPlan.resultCount}장 · 원본사진 ${retryPlan.originalCount}장 · 총 ${retryPlan.items.size}개",
                        )
                        errorMessage?.let { Text(it, style = typography.Body, color = colors.Destructive) }
                        Surface(color = colors.DestructiveSoft, shape = MaterialTheme.shapes.medium) {
                            Text("삭제한 사진은 되돌릴 수 없습니다.",
                                modifier = Modifier.padding(spacing.controlGap),
                                style = typography.Secondary, color = colors.TextPrimary)
                        }
                    }
                    errorMessage != null -> {
                        Text(errorMessage, style = typography.Body, color = colors.Destructive)
                    }
                    else -> {
                        Text("삭제할 사진과 원본 포함 여부를 확인해 주세요.",
                            style = typography.Body, color = colors.TextSecondary)
                        GalleryFolderInfoCard(
                            name = "선택한 사진",
                            detail = "사진 ${selected.distinctBy { it.id }.size}장 · 연결된 원본 ${originals.size}장",
                        )
                        if (allowIncludeOriginals && originals.isNotEmpty()) {
                            DDZButton(
                                text = if (!includeOriginals) "✓ 선택한 사진만 삭제" else "선택한 사진만 삭제",
                                onClick = { includeOriginals = false },
                                style = if (!includeOriginals) DDZButtonStyle.Primary else DDZButtonStyle.Secondary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DDZButton(
                                text = if (includeOriginals) "✓ 원본 포함 삭제" else "원본 포함 삭제",
                                onClick = { includeOriginals = true },
                                style = if (includeOriginals) DDZButtonStyle.Primary else DDZButtonStyle.Secondary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        plan?.let { Text("총 삭제 파일 ${it.items.size}개",
                            style = typography.Body, color = colors.TextPrimary) }
                        Surface(color = colors.DestructiveSoft, shape = MaterialTheme.shapes.medium) {
                            Text("삭제한 사진은 되돌릴 수 없습니다.",
                                modifier = Modifier.padding(spacing.controlGap),
                                style = typography.Secondary, color = colors.TextPrimary)
                        }
                        prepared?.exceptionOrNull()?.message?.let {
                            Text(it, color = colors.Destructive, style = typography.Secondary)
                        }
                    }
                }
            }
        },
        actions = {
            when {
                busy -> Unit
                retryPlan != null -> {
                    DDZButton(text = "취소", onClick = onCancel,
                        modifier = Modifier.weight(1f), style = DDZButtonStyle.Secondary)
                    DDZButton(text = "삭제 재시도", onClick = onRetry,
                        modifier = Modifier.weight(1f), style = DDZButtonStyle.Destructive)
                }
                errorMessage != null -> {
                    DDZButton(text = "닫기", onClick = onCancel,
                        modifier = Modifier.weight(1f), style = DDZButtonStyle.Secondary)
                }
                else -> {
                    DDZButton(text = "취소", onClick = onCancel,
                        modifier = Modifier.weight(1f), style = DDZButtonStyle.Secondary)
                    DDZButton(
                        text = "삭제", onClick = { plan?.let(onConfirm) },
                        modifier = Modifier.weight(1f),
                        style = DDZButtonStyle.Primary,
                        containerColorOverride = colors.Destructive,
                        enabled = plan != null,
                    )
                }
            }
        },
    )
}
