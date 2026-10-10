package com.dudoziworkshop.dzlog.ui.log

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.GalleryFolderCatalog
import com.dudoziworkshop.dzlog.data.mediastore.GalleryFolderStorage
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoMover
import com.dudoziworkshop.dzlog.data.mediastore.GallerySnapshotMemory
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A frozen viewer selection uses the existing picker, confirmation and verified mover. */
@Composable
internal fun GalleryViewerMoveDialog(
    item: MediaImageItem,
    reader: DzlogMediaStoreReader,
    onVerified: (Set<Long>) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog = remember(context) { GalleryFolderCatalog(context.applicationContext) }
    val storage = remember(context) { GalleryFolderStorage(context.applicationContext) }
    var snapshot by remember(item) { mutableStateOf<List<MediaImageItem>>(emptyList()) }
    var folders by remember(item) { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember(item) { mutableStateOf(true) }
    var loadError by remember(item) { mutableStateOf<String?>(null) }
    var loadVersion by remember(item) { mutableStateOf(0) }
    var picking by remember(item) { mutableStateOf(true) }
    var destination by remember(item) { mutableStateOf(GalleryFolderIndexPolicy.ROOT) }
    var busy by remember(item) { mutableStateOf(false) }
    var pending by remember(item) { mutableStateOf<GalleryPhotoMovePlan?>(null) }
    var retryPlan by remember(item) { mutableStateOf<GalleryPhotoMovePlan?>(null) }
    var error by remember(item) { mutableStateOf<String?>(null) }

    LaunchedEffect(item, loadVersion) {
        loading = true
        loadError = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val images = reader.loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT)
                require(images.singleOrNull { it.id == item.id } == item) {
                    "선택한 사진 정보가 변경됐습니다. 취소 후 갤러리에서 다시 확인해 주세요."
                }
                val paths = (images.map { it.relativePath } + catalog.listDescendantPaths(GalleryFolderIndexPolicy.ROOT))
                    .flatMap { path ->
                        buildList {
                            var current: String? = path
                            while (current != null && current.startsWith(GalleryFolderIndexPolicy.ROOT)) {
                                if (!GalleryPhotoMovePolicy.isOriginalPath(current)) add(current)
                                current = GalleryFolderIndexPolicy.parentOf(current)
                            }
                        }
                    }.plus(GalleryFolderIndexPolicy.ROOT).distinct().sorted()
                images to paths
            }
        }
        result.onSuccess { (images, paths) -> snapshot = images; folders = paths }
            .onFailure {
                if (it is CancellationException) throw it
                loadError = it.message ?: "이동할 폴더를 불러오지 못했습니다."
            }
        loading = false
    }

    fun execute(plan: GalleryPhotoMovePlan) {
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { GalleryPhotoMover(context.contentResolver).move(plan) }
            }
            pending = null
            busy = false
            result.onSuccess { moved ->
                GallerySnapshotMemory.cache.invalidate()
                if (moved.movedIds.isNotEmpty()) onVerified(moved.movedIds)
                val remaining = plan.items.filter { it.id in moved.failedIds }
                retryPlan = if (remaining.isEmpty()) null else plan.copy(
                    items = remaining,
                    destinations = plan.destinations.filterKeys { it in moved.failedIds },
                    pairedOriginalCount = remaining.count { GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) },
                )
                if (remaining.isEmpty()) onClose()
                else error = "이동 ${moved.moved}개 · 미완료 ${moved.failed}개. ${moved.detail.orEmpty()}"
            }.onFailure {
                if (it is CancellationException) throw it
                retryPlan = plan
                error = it.message ?: "사진 이동 결과를 확인하지 못했습니다."
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val plan = pending
        if (result.resultCode == Activity.RESULT_OK && plan != null) execute(plan)
        else {
            pending = null
            busy = false
            error = "사진 수정 권한이 허용되지 않아 이동을 취소했습니다."
        }
    }
    fun request(plan: GalleryPhotoMovePlan) {
        if (busy || pending != null) return
        retryPlan = plan
        error = null
        busy = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                pending = plan
                val intent = MediaStore.createWriteRequest(context.contentResolver, plan.items.map { it.uri })
                permission.launch(IntentSenderRequest.Builder(intent.intentSender).build())
            }.onFailure {
                pending = null
                busy = false
                error = "사진 수정 권한 요청을 열지 못했습니다."
            }
        } else execute(plan)
    }
    BackHandler(enabled = busy || pending != null) { }
    when {
        busy || error != null -> AlertDialog(
            onDismissRequest = { if (!busy) onClose() },
            title = { Text(if (busy) "사진 이동 중" else "미완료 파일 이동 확인") },
            text = {
                Text(if (busy) "Android 승인 및 파일 이동 결과를 확인하고 있습니다."
                else "${error.orEmpty()}\n총 재시도 파일 ${retryPlan?.items?.size ?: 0}개\n${retryPlan?.destinations?.values?.distinct()?.joinToString("\n").orEmpty()}")
            },
            confirmButton = {
                if (!busy) retryPlan?.let { plan -> TextButton(onClick = { request(plan) }) { Text("재시도") } }
            },
            dismissButton = { if (!busy) TextButton(onClick = onClose) { Text("취소") } },
        )
        picking -> GalleryMoveDestinationPicker(
            allPhotos = snapshot,
            knownFolderPaths = folders,
            selectedIds = setOf(item.id),
            initialPath = destination,
            loading = loading,
            loadError = loadError,
            onRetry = { loadVersion++ },
            loadPhysicalChildren = { path -> withContext(Dispatchers.IO) { storage.listImmediateFolderPaths(path) } },
            onCancel = onClose,
            onChoose = { destination = it; picking = false },
        )
        else -> GalleryPhotoMoveConfirmation(
            selected = listOf(item),
            allPhotos = snapshot,
            destinationPath = destination,
            busy = false,
            onChangeDestination = { picking = true },
            onCancel = onClose,
            onConfirm = ::request,
        )
    }
}
