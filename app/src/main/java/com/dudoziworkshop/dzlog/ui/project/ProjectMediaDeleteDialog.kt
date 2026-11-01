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
import com.dudoziworkshop.dzlog.ui.log.GallerySelectedPhotoDeleteDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Reuse the gallery's Android delete request + verified result path.
 * A cancelled/partial deletion NEVER removes project metadata.
 */
@Composable
internal fun ProjectMediaDeleteDialog(
    projectId: String,
    onPrepare: suspend (String) -> PreparedProjectMediaDeletion,
    onFinish: suspend (PreparedProjectMediaDeletion, Set<Long>) -> Boolean,
    onDeleted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val reader = remember(context) { DzlogMediaStoreReader(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var prepared by remember(projectId) { mutableStateOf<PreparedProjectMediaDeletion?>(null) }
    var error by remember(projectId) { mutableStateOf<String?>(null) }
    var revision by remember(projectId) { mutableIntStateOf(0) }
    var deleteRequested by remember(projectId) { mutableStateOf(false) }
    var finalizing by remember(projectId) { mutableStateOf(false) }
    var verifiedIds by remember(projectId) { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(projectId, revision) {
        try {
            prepared = onPrepare(projectId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "삭제 대상을 확인할 수 없습니다."
        }
    }

    fun finish(ready: PreparedProjectMediaDeletion, ids: Set<Long>) {
        if (finalizing) return
        finalizing = true
        scope.launch {
            try {
                if (onFinish(ready, ids)) {
                    onDeleted()
                } else {
                    error = "연결된 사진이 남아 있습니다. 실패한 파일은 보존했으며 다시 확인할 수 있습니다."
                    prepared = null
                    revision++
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "삭제 확인에 실패했습니다. 프로젝트는 유지됩니다."
                prepared = null
                revision++
            } finally {
                finalizing = false
            }
        }
    }

    val selected = prepared
    if (deleteRequested && selected != null && selected.photos.isNotEmpty()) {
        GallerySelectedPhotoDeleteDialog(
            selected = selected.photos,
            reader = reader,
            onVerified = { ids -> verifiedIds = verifiedIds + ids },
            onClose = {
                deleteRequested = false
                val confirmed = verifiedIds
                if (confirmed.isNotEmpty()) {
                    finish(selected, confirmed)
                } else {
                    onDismiss()
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!finalizing) onDismiss() },
        title = { Text("프로젝트와 사진 삭제") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val count = selected?.photos?.size
                Text(if (count == null) "삭제 대상 확인 중…" else
                    "이 프로젝트에 연결된 사진 " + count + "개를 삭제합니다.")
                Text("저장 폴더 전체가 아닌, 프로젝트에 연결된 원본·결과 사진만 대상으로 합니다.")
                Text("삭제한 사진은 되돌릴 수 없습니다.", color = DDZColor.Destructive)
                error?.let { Text(it, color = DDZColor.Destructive) }
                if (finalizing) Text("Android 삭제 결과 및 프로젝트 기록 확인 중…")
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null && !finalizing,
                onClick = {
                    val ready = selected ?: return@TextButton
                    if (ready.photos.isEmpty()) {
                        finish(ready, emptySet())
                    } else {
                        verifiedIds = emptySet()
                        deleteRequested = true
                    }
                },
            ) { Text("사진 및 프로젝트 삭제") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !finalizing) { Text("취소") }
        },
    )
}
