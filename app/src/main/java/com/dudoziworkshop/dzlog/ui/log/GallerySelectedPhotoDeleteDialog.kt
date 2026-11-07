package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletion
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared frozen selection, confirmation and failed-file retry flow for grids and the viewer. */
@Composable
internal fun GallerySelectedPhotoDeleteDialog(
    selected: List<MediaImageItem>,
    reader: DzlogMediaStoreReader,
    allowIncludeOriginals: Boolean = true,
    onVerified: (Set<Long>) -> Unit,
    onClose: () -> Unit,
) {
    var loading by remember(selected) { mutableStateOf(true) }
    var snapshot by remember(selected) { mutableStateOf<List<MediaImageItem>?>(null) }
    var retry by remember(selected) { mutableStateOf<GalleryPhotoDeletePlan?>(null) }
    var error by remember(selected) { mutableStateOf<String?>(null) }
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
    LaunchedEffect(selected) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                reader.loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT).also {
                    GalleryPhotoDeletion.prepare(selected, it, includeOriginals = false)
                }
            }
        }
        result.onSuccess { snapshot = it }
            .onFailure { error = it.message ?: "삭제 대상을 확인하지 못했습니다." }
        loading = false
    }
    // Keep ONE modern DZlog dialog mounted across lookup, confirmation,
    // Android permission/verification, and failed-file retry. The old
    // DDZAlertDialog branch used to flash before the confirmation dialog.
    GalleryPhotoDeleteConfirmation(
        selected = selected,
        allPhotos = snapshot.orEmpty(),
        busy = loading || action.busy,
        allowIncludeOriginals = allowIncludeOriginals,
        loading = loading,
        retryPlan = retry,
        errorMessage = error,
        progressText = action.progressText,
        onCancel = onClose,
        onConfirm = { plan ->
            if (!loading && snapshot != null && !action.busy && retry == null) {
                retry = plan
                error = null
                action.request(plan)
            }
        },
        onRetry = {
            val pending = retry
            if (!loading && !action.busy && pending != null) {
                error = null
                action.request(pending)
            }
        },
    )
}
