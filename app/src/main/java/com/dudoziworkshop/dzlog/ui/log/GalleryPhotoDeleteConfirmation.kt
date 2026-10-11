package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Presents a deletion plan only; the caller owns permission requests and execution. */
@Composable
internal fun GalleryPhotoDeleteConfirmation(
    selected: List<MediaImageItem>,
    allPhotos: List<MediaImageItem>,
    busy: Boolean,
    onCancel: () -> Unit,
    onConfirm: (GalleryPhotoDeletePlan) -> Unit,
) {
    val originals = remember(selected, allPhotos) {
        GalleryPhotoMovePolicy.pairedOriginals(selected, allPhotos)
    }
    var includeOriginals by remember(selected, allPhotos) { mutableStateOf<Boolean?>(null) }
    var finalStep by remember(selected, allPhotos) { mutableStateOf(false) }
    val showFinal = originals.isEmpty() || finalStep
    val prepared = remember(selected, allPhotos, includeOriginals) {
        runCatching { GalleryPhotoDeletion.prepare(selected, allPhotos, includeOriginals == true) }
    }
    val plan = prepared.getOrNull()
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        title = { Text(if (showFinal) "삭제 최종 확인" else "사진 삭제 대상") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!showFinal) {
                    Text("선택한 사진 ${selected.distinctBy { it.id }.size}장에 연결된 원본 ${originals.size}장이 있습니다.")
                    Text("삭제할 대상을 선택하세요.")
                    OutlinedButton(enabled = !busy, onClick = { includeOriginals = false }) {
                        Text(if (includeOriginals == false) "✓ 선택한 사진만 삭제" else "선택한 사진만 삭제")
                    }
                    OutlinedButton(enabled = !busy, onClick = { includeOriginals = true }) {
                        Text(if (includeOriginals == true) "✓ 원본 포함 삭제" else "원본 포함 삭제")
                    }
                } else if (plan != null) {
                    Text("결과사진 ${plan.resultCount}장")
                    Text("원본사진 ${plan.originalCount}장")
                    Text("총 삭제 파일 ${plan.items.size}개")
                    Text("삭제한 사진은 되돌릴 수 없습니다.", color = DDZColor.Destructive)
                }
                prepared.exceptionOrNull()?.message?.let { Text(it, color = DDZColor.Destructive) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && plan != null && (showFinal || includeOriginals != null),
                onClick = {
                    if (showFinal) onConfirm(requireNotNull(plan)) else finalStep = true
                },
            ) { Text(if (showFinal) "삭제" else "다음", color = if (!busy && plan != null && showFinal) DDZColor.Destructive else DDZColor.TextSecondary) }
        },
        dismissButton = {
            Row {
                if (showFinal && originals.isNotEmpty()) {
                    TextButton(enabled = !busy, onClick = { finalStep = false }) { Text("이전") }
                }
                TextButton(enabled = !busy, onClick = onCancel) { Text("취소") }
            }
        },
    )
}
