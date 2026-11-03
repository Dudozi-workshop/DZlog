package com.dudoziworkshop.dzlog.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.project.PreparedProjectMediaDeletion
import com.dudoziworkshop.dzlog.ui.log.GallerySelectedPhotoDeleteDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Project deletion uses the gallery's Android per-file permission and
 * verified-result retry UI. Closing an incomplete deletion retains the
 * project; only the coordinator can finalize metadata removal.
 */
@Composable
internal fun ProjectMediaDeleteDialog(
    projectId: String,
    onPrepare: suspend (String) -> PreparedProjectMediaDeletion,
    onFinish: suspend (PreparedProjectMediaDeletion) -> Boolean,
    onDeleted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val reader = remember(context) { DzlogMediaStoreReader(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var prepared by remember(projectId) { mutableStateOf<PreparedProjectMediaDeletion?>(null) }
    var message by remember(projectId) { mutableStateOf<String?>(null) }
    var revision by remember(projectId) { mutableIntStateOf(0) }
    var showGalleryDeletion by remember(projectId) { mutableStateOf(false) }
    var verifying by remember(projectId) { mutableStateOf(false) }
    // Read mutable state directly: onVerified and onClose may happen before recomposition.
    val verifiedIds = remember(projectId) { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(projectId, revision) {
        prepared = null
        try {
            prepared = onPrepare(projectId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            message = failure.message ?: "삭제 대상을 확인하지 못했습니다."
        }
    }

    fun verifyAndFinish(snapshot: PreparedProjectMediaDeletion) {
        if (verifying) return
        verifying = true
        scope.launch {
            try {
                if (onFinish(snapshot)) {
                    onDeleted()
                } else {
                    message = "삭제되지 않은 사진이 있습니다. 프로젝트와 남은 사진은 유지됩니다."
                    prepared = null
                    revision++
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                message = failure.message ?: "삭제 확인에 실패했습니다. 프로젝트는 유지됩니다."
                prepared = null
                revision++
            } finally {
                verifying = false
            }
        }
    }

    val snapshot = prepared
    if (showGalleryDeletion && snapshot != null && snapshot.photos.isNotEmpty()) {
        GallerySelectedPhotoDeleteDialog(
            selected = snapshot.photos,
            reader = reader,
            onVerified = { ids -> verifiedIds.value = verifiedIds.value + ids },
            onClose = {
                showGalleryDeletion = false
                if (verifiedIds.value.isEmpty()) {
                    onDismiss()
                } else {
                    verifyAndFinish(snapshot)
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!verifying) onDismiss() },
        title = { Text("프로젝트 및 연결 사진 삭제") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (snapshot == null) "연결된 사진 확인 중…"
                    else "프로젝트에 연결된 사진 \${snapshot.fileCount}개를 삭제합니다.",
                )
                Text("저장 폴더 전체는 삭제하지 않으며 다른 프로젝트 사진은 유지합니다.")
                Text("일부 사진 삭제가 실패하면 프로젝트와 남은 사진을 유지합니다.")
                Text("삭제한 사진은 복구할 수 없습니다.", color = DDZColor.Destructive)
                if (verifying) Text("삭제된 파일과 프로젝트 상태를 확인 중…")
                message?.let { Text(it, color = DDZColor.Destructive) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = snapshot != null && !verifying,
                onClick = {
                    val selected = snapshot ?: return@TextButton
                    if (selected.photos.isEmpty()) {
                        verifyAndFinish(selected)
                    } else {
                        verifiedIds.value = emptySet()
                        showGalleryDeletion = true
                    }
                },
            ) { Text("프로젝트 및 사진 삭제") }
        },
        dismissButton = {
            TextButton(enabled = !verifying, onClick = onDismiss) { Text("취소") }
        },
    )
}
