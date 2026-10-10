package com.dudoziworkshop.dzlog.ui.log

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeleteResult
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class GalleryPhotoDeleteAction(
    val busy: Boolean,
    val request: (GalleryPhotoDeletePlan) -> Unit,
)

/** Validates before permission and verifies after deletion; cancellation never clears selection. */
@Composable
internal fun rememberGalleryPhotoDeleteAction(
    onResult: (GalleryPhotoDeletePlan, GalleryPhotoDeleteResult) -> Unit,
    onError: (String) -> Unit,
): GalleryPhotoDeleteAction {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deletion = remember(context) { GalleryPhotoDeletion(context.contentResolver) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<GalleryPhotoDeletePlan?>(null) }

    fun verify(plan: GalleryPhotoDeletePlan) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { deletion.verifyResult(plan) } }
            pending = null
            busy = false
            result.onSuccess { onResult(plan, it) }
                .onFailure { onError(it.message ?: "삭제 결과를 확인하지 못했습니다.") }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val plan = pending
        if (result.resultCode == Activity.RESULT_OK && plan != null) {
            // createDeleteRequest performs the deletion itself after Android confirmation.
            verify(plan)
        } else {
            pending = null
            busy = false
            onError("삭제를 취소했습니다. 선택한 사진은 유지됩니다.")
        }
    }
    return GalleryPhotoDeleteAction(busy) { plan ->
        if (!busy && pending == null) {
            busy = true
            pending = plan
            scope.launch {
                val checked = withContext(Dispatchers.IO) { runCatching { deletion.pendingItems(plan) } }
                val items = checked.getOrNull()
                if (items == null) {
                    pending = null
                    busy = false
                    onError(checked.exceptionOrNull()?.message ?: "삭제 대상을 확인하지 못했습니다.")
                } else if (items.isEmpty()) {
                    verify(plan)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    runCatching {
                        val request = MediaStore.createDeleteRequest(context.contentResolver, items.map { it.uri })
                        permission.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    }.onFailure {
                        pending = null
                        busy = false
                        onError("Android 삭제 확인 화면을 열지 못했습니다.")
                    }
                } else {
                    // Android 10: only directly writable files; denied items remain retry targets.
                    withContext(Dispatchers.IO) {
                        items.forEach { item -> runCatching { context.contentResolver.delete(item.uri, null, null) } }
                    }
                    verify(plan)
                }
            }
        }
    }
}
