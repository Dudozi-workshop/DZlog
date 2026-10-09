package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps failed originals retryable even when their result photo has left the viewer. */
@Composable
internal fun GalleryViewerDeleteDialog(
    item: MediaImageItem,
    reader: DzlogMediaStoreReader,
    onVerified: (Set<Long>) -> Unit,
    onClose: () -> Unit,
) {
    var loading by remember(item) { mutableStateOf(true) }
    var snapshot by remember(item) { mutableStateOf<List<MediaImageItem>?>(null) }
    var retry by remember(item) { mutableStateOf<GalleryPhotoDeletePlan?>(null) }
    var error by remember(item) { mutableStateOf<String?>(null) }
    val action = rememberGalleryPhotoDeleteAction(
        onResult = { plan, result ->
            val failed = plan.items.filter { it.id in result.failedIds }
            retry = if (failed.isEmpty()) null else plan.copy(items = failed)
            onVerified(result.deletedIds)
            if (failed.isEmpty()) onClose() else error = result.detail
        },
        onError = { error = it },
    )
    val busy = loading || action.busy
    LaunchedEffect(item) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                reader.loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT).also {
                    GalleryPhotoDeletion.prepare(listOf(item), it, includeOriginals = false)
                }
            }
        }
        result.onSuccess { snapshot = it }
            .onFailure { error = it.message ?: "삭제 대상을 확인하지 못했습니다." }
        loading = false
    }
    val photos = snapshot
    val pending = retry
    if (photos != null && pending == null && !busy) {
        GalleryPhotoDeleteConfirmation(
            selected = listOf(item),
            allPhotos = photos,
            busy = false,
            onCancel = onClose,
            onConfirm = { plan ->
                if (!action.busy && retry == null) {
                    retry = plan
                    error = null
                    action.request(plan)
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = { if (!busy) onClose() },
            title = { Text(if (busy) "사진 삭제" else "미완료 파일 삭제 확인") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (busy) Text(if (loading) "삭제 대상 확인 중…" else "Android 확인 및 삭제 결과 확인 중…")
                    pending?.let {
                        Text("결과사진 ${it.resultCount}장 · 원본사진 ${it.originalCount}장")
                        Text("총 재시도 파일 ${it.items.size}개")
                        Text("삭제한 사진은 되돌릴 수 없습니다.", color = DDZColor.Destructive)
                    }
                    error?.let { Text(it, color = DDZColor.Destructive) }
                    if (!busy && pending != null) {
                        Text("삭제되지 않은 파일만 다시 요청합니다.")
                    }
                }
            },
            confirmButton = {
                if (pending != null) {
                    TextButton(enabled = !busy, onClick = {
                        if (!busy) {
                            error = null
                            action.request(pending)
                        }
                    }) { Text("삭제 재시도", color = DDZColor.Destructive) }
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { if (!busy) onClose() }) { Text("취소") }
            },
        )
    }
}
