package com.dudoziworkshop.dzlog.ui.log

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeleteResult
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryDeleteBatchPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class GalleryPhotoDeleteAction(
    val busy: Boolean,
    val request: (GalleryPhotoDeletePlan) -> Unit,
    val progressText: String? = null,
)

private enum class DeletePhase { IDLE, PREPARE, WAITING_FOR_ANDROID, VERIFY }

private class DeleteSession(val plan: GalleryPhotoDeletePlan) {
    var offset = 0
    var currentBatch: List<MediaImageItem> = emptyList()
    val deletedIds = mutableSetOf<Long>()

    val progressText: String get() =
        "삭제 확인 및 처리 중: ${(offset + currentBatch.size).coerceAtMost(plan.items.size)}/${plan.items.size}개"
}

/** One Android permission request per batch, then verification before the next request. */
@Composable
internal fun rememberGalleryPhotoDeleteAction(
    onResult: (GalleryPhotoDeletePlan, GalleryPhotoDeleteResult) -> Unit,
    onError: (String) -> Unit,
): GalleryPhotoDeleteAction {
    val context = LocalContext.current
    val deletion = remember(context) { GalleryPhotoDeletion(context.contentResolver) }
    var session by remember { mutableStateOf<DeleteSession?>(null) }
    var phase by remember { mutableStateOf(DeletePhase.IDLE) }

    fun finish(current: DeleteSession, reason: String? = null) {
        if (session !== current) return
        session = null
        phase = DeletePhase.IDLE
        val deleted = current.deletedIds.toSet()
        if (reason != null && deleted.isEmpty()) {
            onError(reason)
            return
        }
        val failed = current.plan.items.mapTo(mutableSetOf()) { it.id } - deleted
        onResult(
            current.plan,
            GalleryPhotoDeleteResult(
                deletedIds = deleted,
                failedIds = failed,
                detail = if (failed.isEmpty()) null
                    else reason ?: "삭제되지 않은 파일 ${failed.size}개가 남아 있습니다.",
            ),
        )
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val current = session
        if (current != null && phase == DeletePhase.WAITING_FOR_ANDROID) {
            if (result.resultCode == Activity.RESULT_OK) {
                phase = DeletePhase.VERIFY
            } else {
                finish(current, "삭제가 중단됐습니다. 완료되지 않은 사진은 그대로 유지됩니다.")
            }
        }
    }

    LaunchedEffect(session, phase) {
        val current = session ?: return@LaunchedEffect
        when (phase) {
            DeletePhase.PREPARE -> {
                val batch = GalleryDeleteBatchPolicy.batchAt(current.plan.items, current.offset)
                if (batch.isEmpty()) {
                    finish(current)
                    return@LaunchedEffect
                }
                current.currentBatch = batch
                val checked = withContext(Dispatchers.IO) {
                    runCatching { deletion.pendingItems(GalleryPhotoDeletePlan(batch)) }
                }
                val remaining = checked.getOrNull()
                if (remaining == null) {
                    finish(current, checked.exceptionOrNull()?.message ?: "삭제 대상을 확인하지 못했습니다.")
                } else if (remaining.isEmpty()) {
                    phase = DeletePhase.VERIFY
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    runCatching {
                        val request = MediaStore.createDeleteRequest(
                            context.contentResolver, remaining.map { it.uri }
                        )
                        phase = DeletePhase.WAITING_FOR_ANDROID
                        permission.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    }.onFailure {
                        finish(current, "Android 삭제 확인 화면을 열지 못했습니다.")
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        remaining.forEach { item ->
                            runCatching { context.contentResolver.delete(item.uri, null, null) }
                        }
                    }
                    phase = DeletePhase.VERIFY
                }
            }
            DeletePhase.VERIFY -> {
                val verified = withContext(Dispatchers.IO) {
                    runCatching { deletion.verifyResult(GalleryPhotoDeletePlan(current.currentBatch)) }
                }
                val result = verified.getOrNull()
                if (result == null) {
                    finish(current, verified.exceptionOrNull()?.message ?: "삭제 결과 확인에 실패했습니다.")
                } else {
                    current.deletedIds += result.deletedIds
                    if (result.failedIds.isNotEmpty()) {
                        finish(current, result.detail ?: "일부 파일의 삭제를 확인하지 못했습니다.")
                    } else {
                        current.offset += current.currentBatch.size
                        current.currentBatch = emptyList()
                        phase = DeletePhase.PREPARE
                    }
                }
            }
            DeletePhase.IDLE, DeletePhase.WAITING_FOR_ANDROID -> Unit
        }
    }

    return GalleryPhotoDeleteAction(
        busy = session != null,
        progressText = session?.progressText,
        request = { plan ->
            if (session == null) {
                session = DeleteSession(plan)
                phase = DeletePhase.PREPARE
            }
        },
    )
}
