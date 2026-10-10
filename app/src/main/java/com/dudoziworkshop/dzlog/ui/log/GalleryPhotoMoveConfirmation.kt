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
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Optional original choice followed by a separate, mandatory final confirmation. */
@Composable
internal fun GalleryPhotoMoveConfirmation(
    selected: List<MediaImageItem>,
    allPhotos: List<MediaImageItem>,
    destinationPath: String,
    busy: Boolean,
    onChangeDestination: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: (GalleryPhotoMovePlan) -> Unit,
) {
    val originals = remember(selected, allPhotos) { GalleryPhotoMovePolicy.pairedOriginals(selected, allPhotos) }
    var includeOriginals by remember(selected, destinationPath) { mutableStateOf<Boolean?>(null) }
    var finalStep by remember(selected, destinationPath) { mutableStateOf(false) }
    val showFinal = originals.isEmpty() || finalStep
    val planned = remember(selected, originals, destinationPath, includeOriginals) {
        runCatching { GalleryPhotoMovePolicy.plan(selected, originals, destinationPath, includeOriginals == true) }
    }
    val plan = planned.getOrNull()
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        title = { Text(if (showFinal) "이동 최종 확인" else "사진 이동 대상") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("이동 위치")
                Text(destinationPath)
                if (!showFinal) {
                    Text("선택한 사진 ${selected.size}장에 연결된 원본 ${originals.size}장이 있습니다.")
                    Text("함께 이동할 대상을 선택하세요.")
                    OutlinedButton(enabled = !busy, onClick = { includeOriginals = false }) {
                        Text(if (includeOriginals == false) "✓ 사진만 이동" else "사진만 이동")
                    }
                    OutlinedButton(enabled = !busy, onClick = { includeOriginals = true }) {
                        Text(if (includeOriginals == true) "✓ 원본 포함 이동" else "원본 포함 이동")
                    }
                } else if (plan != null) {
                    val originalCount = plan.items.count { GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) }
                    Text("결과사진 ${plan.items.size - originalCount}장")
                    Text("원본사진 ${originalCount}장")
                    Text("총 이동 파일 ${plan.items.size}개")
                    Text("확인 후 필요한 Android 사진 수정 권한을 요청합니다.", color = DDZColor.TextSecondary)
                }
                planned.exceptionOrNull()?.message?.let { Text(it, color = DDZColor.Destructive) }
                Text("촬영 저장설정은 변경되지 않습니다.", color = DDZColor.TextSecondary)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && plan != null && (showFinal || includeOriginals != null), onClick = {
                if (showFinal) onConfirm(requireNotNull(plan)) else finalStep = true
            }) { Text(if (showFinal) "이동" else "다음") }
        },
        dismissButton = {
            Row {
                TextButton(enabled = !busy, onClick = {
                    if (showFinal && originals.isNotEmpty()) finalStep = false else onChangeDestination()
                }) { Text("이전") }
                TextButton(enabled = !busy, onClick = onCancel) { Text("취소") }
            }
        },
    )
}
