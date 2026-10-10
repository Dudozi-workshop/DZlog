package com.dudoziworkshop.dzlog.ui.log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.favorites.FavoritesProvider
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.GalleryFolderCatalog
import com.dudoziworkshop.dzlog.data.mediastore.GalleryFolderStorage
import com.dudoziworkshop.dzlog.data.mediastore.GallerySnapshotMemory
import com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoMover
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.feature.log.policy.CapturePathImpact
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderOperationPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndex
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummary
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** One screen for every folder depth; the route state is the actual relative path. */
@Composable
fun LogFolderScreen(
    relativePath: String,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPhoto: (List<MediaImageItem>, Int) -> Unit,
    onOpenOriginal: (String) -> Unit,
    capturePathDrafts: List<List<TableEditorSlotDraft?>> = emptyList(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember(context) { DzlogMediaStoreReader(context.contentResolver) }
    val folderStorage = remember(context) { GalleryFolderStorage(context.applicationContext) }
    val folderCatalog = remember(context) { GalleryFolderCatalog(context.applicationContext) }
    val favoriteIds by remember(context) { FavoritesProvider.repo(context) }.favoriteIdsFlow.collectAsState(initial = emptySet())
    var selectedTab by remember { mutableStateOf(GalleryTab.ALL) }
    var showRecentPhotos by rememberSaveable(relativePath) { mutableStateOf(false) }
    val cached = remember(relativePath) { GallerySnapshotMemory.cache.get(relativePath) }
    var allPhotos by remember(relativePath) { mutableStateOf(cached?.allPhotos ?: emptyList()) }
    var index by remember(relativePath) { mutableStateOf<GalleryFolderIndex?>(cached?.index) }
    var summariesByPath by remember(relativePath) {
        mutableStateOf<Map<String, GalleryFolderSummary>>(cached?.summariesByPath ?: emptyMap())
    }
    var photos by remember(relativePath) { mutableStateOf(cached?.directPhotos ?: emptyList()) }
    var photoSort by rememberSaveable(relativePath) { mutableStateOf(GalleryPhotoSort.NEWEST) }
    var showPhotoSort by remember(relativePath) { mutableStateOf(false) }
    val sortedPhotos = remember(photos, photoSort) { photoSort.sorted(photos) }
    var error by remember(relativePath) { mutableStateOf<String?>(null) }
    var loading by remember(relativePath) { mutableStateOf(cached == null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var manageAction by remember { mutableStateOf<String?>(null) }
    var managedFolderPath by remember(relativePath) { mutableStateOf(relativePath) }
    var folderManagementBusy by remember { mutableStateOf(false) }
    var renameName by remember { mutableStateOf("") }
    var moveTarget by remember { mutableStateOf(GalleryFolderIndexPolicy.ROOT) }
    var destinationPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var managementConfirmed by remember { mutableStateOf(false) }
    var folderOperationError by remember { mutableStateOf<String?>(null) }
    var connected by remember { mutableStateOf(folderStorage.isConnected()) }
    var showFolderAccessHelp by remember { mutableStateOf(false) }
    var folderAccessBusy by remember { mutableStateOf(false) }
    var selectedPhotoIds by remember(relativePath) { mutableStateOf<Set<Long>>(emptySet()) }
    var selectionActive by remember(relativePath) { mutableStateOf(false) }
    var showPhotoMove by remember { mutableStateOf(false) }
    var showMoveDestination by remember { mutableStateOf(false) }
    var photoMoveLoading by remember { mutableStateOf(false) }
    var photoMoveLoadVersion by remember { mutableStateOf(0) }
    var photoMoveLoadError by remember { mutableStateOf<String?>(null) }
    var photoMoveTarget by remember { mutableStateOf(GalleryFolderIndexPolicy.ROOT) }
    var photoMoveFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var photoMoveBusy by remember { mutableStateOf(false) }
    var moveAllPhotos by remember { mutableStateOf<List<MediaImageItem>>(emptyList()) }
    var pendingPhotoPlan by remember { mutableStateOf<com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan?>(null) }
    var retryPhotoPlan by remember { mutableStateOf<com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan?>(null) }
    var showPhotoMoveRetry by remember { mutableStateOf(false) }

    var reloadKey by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var showPhotoDelete by remember { mutableStateOf(false) }
    var photoDeleteLoading by remember { mutableStateOf(false) }
    var deleteSnapshot by remember { mutableStateOf<List<MediaImageItem>>(emptyList()) }
    var deleteSelected by remember { mutableStateOf<List<MediaImageItem>>(emptyList()) }
    var retryDeletePlan by remember { mutableStateOf<com.dudoziworkshop.dzlog.data.mediastore.GalleryPhotoDeletePlan?>(null) }
    var showDeleteRetry by remember { mutableStateOf(false) }
    val deleteAction = rememberGalleryPhotoDeleteAction(
        onResult = { plan, result ->
            val failed = plan.items.filter { it.id in result.failedIds }
            retryDeletePlan = if (failed.isEmpty()) null else plan.copy(items = failed)
            selectedPhotoIds = selectedPhotoIds - result.deletedIds
            selectionActive = selectedPhotoIds.isNotEmpty() || result.failedIds.isNotEmpty()
            GallerySnapshotMemory.cache.invalidate()
            reloadKey++
            folderOperationError = if (result.failedIds.isEmpty()) "파일 ${result.deletedIds.size}개 삭제 완료" else result.detail
        },
        onError = { folderOperationError = it },
    )
    val deleteBusy = deleteAction.busy || photoDeleteLoading
    val operationLocked = deleteBusy || photoMoveBusy || pendingPhotoPlan != null || folderManagementBusy
    val navigationLocked = operationLocked || manageAction != null || showPhotoDelete || showPhotoMove
    val folderAccess = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri == null) {
            folderAccessBusy = false
            folderOperationError = "폴더 접근 연결을 취소했습니다."
        } else {
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { folderStorage.connect(treeUri) } }
                folderAccessBusy = false
                connected = folderStorage.isConnected()
                result.onSuccess {
                    folderOperationError = null
                    GallerySnapshotMemory.cache.invalidate()
                    reloadKey++
                }.onFailure {
                    folderOperationError = it.message ?: "폴더 접근 권한을 연결하지 못했습니다."
                }
            }
        }
    }
    fun finishPhotoMove(plan: com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan) {
        photoMoveBusy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { GalleryPhotoMover(context.contentResolver).move(plan) }
            }
            photoMoveBusy = false
            GallerySnapshotMemory.cache.invalidate()
            reloadKey++
            result.onSuccess { moved ->
                selectedPhotoIds = selectedPhotoIds - moved.movedIds
                val failedItems = plan.items.filter { it.id in moved.failedIds }
                retryPhotoPlan = if (failedItems.isEmpty()) null else plan.copy(
                    items = failedItems,
                    destinations = plan.destinations.filterKeys { it in moved.failedIds },
                    pairedOriginalCount = failedItems.count { GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) },
                )
                selectionActive = selectedPhotoIds.isNotEmpty() || moved.failed > 0
                folderOperationError = if (moved.failed > 0) {
                    "이동 ${moved.moved}개 · 미완료 ${moved.failed}개. ${moved.detail.orEmpty()}"
                } else "파일 ${moved.moved}개 이동 완료"
            }.onFailure {
                retryPhotoPlan = plan
                selectionActive = true
                folderOperationError = it.message ?: "사진 이동 실패"
            }
            pendingPhotoPlan = null
        }
    }
    val photoWritePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val pending = pendingPhotoPlan
        if (result.resultCode == Activity.RESULT_OK && pending != null) {
            finishPhotoMove(pending)
        } else {
            pendingPhotoPlan = null
            photoMoveBusy = false
            folderOperationError = "사진 수정 권한이 허용되지 않아 이동을 취소했습니다."
        }
    }

    fun requestPhotoMove(prepared: com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan) {
        if (photoMoveBusy || pendingPhotoPlan != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                pendingPhotoPlan = prepared
                val request = MediaStore.createWriteRequest(context.contentResolver, prepared.items.map { it.uri })
                photoWritePermission.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure {
                pendingPhotoPlan = null
                folderOperationError = "사진 수정 권한 요청을 열지 못했습니다."
            }
        } else finishPhotoMove(prepared)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        connected = folderStorage.isConnected()
        reloadKey++
    }

    LaunchedEffect(reader, relativePath, reloadKey) {
        loading = index == null
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val physicalFolders = if (folderStorage.isConnected())
                    folderStorage.listImmediateFolderPaths(relativePath)
                    else emptyList()
                // Each visible child needs its immediate directories for its summary.
                // A bounded second level is enough; no recursive SAF tree scan is needed.
                val physicalChildFolders = physicalFolders
                    .filterNot { it.trimEnd('/').substringAfterLast('/').equals("original", true) }
                    .flatMap { childPath ->
                        folderStorage.listImmediateFolderPaths(childPath)
                    }
                val catalogFolders = folderCatalog.listDescendantPaths(relativePath)
                reader.loadGallerySnapshot(
                    relativePath,
                    existingFolderPaths = (physicalFolders + physicalChildFolders + catalogFolders).distinct(),
                )
            }
        }
        result.onSuccess { snapshot ->
            GallerySnapshotMemory.cache.put(relativePath, snapshot)
            index = snapshot.index
            summariesByPath = snapshot.summariesByPath
            photos = snapshot.directPhotos
            allPhotos = snapshot.allPhotos
            error = null
        }.onFailure {
            if (it is kotlinx.coroutines.CancellationException) throw it
            error = if (index == null) "목록을 불러오지 못했습니다."
                else "목록 갱신에 실패했습니다. 마지막으로 확인한 목록을 표시합니다."
        }
        loading = false
    }

    fun enterSelection(photo: MediaImageItem) {
        if (operationLocked || showPhotoDelete || showPhotoMove || manageAction != null) return
        selectionActive = true
        selectedPhotoIds = selectedPhotoIds + photo.id
    }
    fun cancelSelection() {
        if (operationLocked || showPhotoDelete || showPhotoMove || manageAction != null) return
        selectedPhotoIds = emptySet()
        selectionActive = false
    }
    fun togglePhoto(photo: MediaImageItem) {
        if (operationLocked || showPhotoDelete || showPhotoMove || manageAction != null) return
        selectionActive = true
        selectedPhotoIds = if (photo.id in selectedPhotoIds) selectedPhotoIds - photo.id else selectedPhotoIds + photo.id
    }
    fun startPhotoDelete() {
        if (selectedPhotoIds.isEmpty() || operationLocked || manageAction != null) return
        val requestedIds = selectedPhotoIds
        retryDeletePlan = null
        folderOperationError = null
        deleteSnapshot = emptyList()
        deleteSelected = emptyList()
        showPhotoDelete = true
        photoDeleteLoading = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val snapshot = reader.loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT)
                    val selected = snapshot.filter { it.id in requestedIds }
                    require(selected.size == requestedIds.size) { "선택한 사진 일부를 찾지 못했습니다. 목록을 다시 확인해 주세요." }
                    snapshot to selected
                }
            }
            photoDeleteLoading = false
            if (!showPhotoDelete) return@launch
            result.onSuccess { (snapshot, selected) -> deleteSnapshot = snapshot; deleteSelected = selected }
                .onFailure { showPhotoDelete = false; folderOperationError = it.message ?: "삭제 대상을 불러오지 못했습니다." }
        }
    }
    fun loadMoveDestinations() {
        val requestVersion = ++photoMoveLoadVersion
        val requestedIds = selectedPhotoIds
        photoMoveLoading = true
        photoMoveLoadError = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val loaded = reader.loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT)
                    require(loaded.count { it.id in requestedIds } == requestedIds.size) {
                        "선택한 사진 일부를 찾지 못했습니다. 취소 후 목록을 다시 확인해 주세요."
                    }
                    val allFolders = loaded.map { it.relativePath } +
                        folderCatalog.listDescendantPaths(GalleryFolderIndexPolicy.ROOT)
                    val paths = mutableSetOf(GalleryFolderIndexPolicy.ROOT)
                    allFolders.forEach { path ->
                        var current = path
                        while (current.startsWith(GalleryFolderIndexPolicy.ROOT)) {
                            if (!GalleryPhotoMovePolicy.isOriginalPath(current)) paths += current
                            current = GalleryFolderIndexPolicy.parentOf(current) ?: break
                        }
                    }
                    loaded to paths.sorted()
                }
            }
            if (requestVersion != photoMoveLoadVersion || !showPhotoMove) return@launch
            result.onSuccess { (images, paths) -> moveAllPhotos = images; photoMoveFolders = paths }
                .onFailure { photoMoveLoadError = it.message ?: "이동할 폴더를 불러오지 못했습니다." }
            photoMoveLoading = false
        }
    }
    fun startPhotoMove() {
        if (selectedPhotoIds.isEmpty() || operationLocked || manageAction != null) return
        folderOperationError = null
        retryPhotoPlan = null
        showPhotoMove = true
        showMoveDestination = true
        photoMoveTarget = GalleryFolderIndexPolicy.ROOT
        moveAllPhotos = emptyList()
        photoMoveFolders = emptyList()
        loadMoveDestinations()
    }
    fun startFolderManagement(path: String, action: String) {
        if (folderManagementBusy || folderAccessBusy || selectionActive || manageAction != null || deleteBusy || photoMoveBusy) return
        if (!connected) {
            showFolderAccessHelp = true
            return
        }
        managedFolderPath = path
        folderOperationError = null
        managementConfirmed = false
        renameName = path.trimEnd('/').substringAfterLast('/')
        moveTarget = GalleryFolderIndexPolicy.ROOT
        destinationPaths = emptyList()
        manageAction = action
        if (action == "move") {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { folderStorage.listDestinationFolders(path) }
                }
                if (managedFolderPath == path && manageAction == action) {
                    result.onSuccess { destinationPaths = it }
                        .onFailure { folderOperationError = it.message ?: "이동할 폴더를 읽지 못했습니다." }
                }
            }
        }
    }
    val root = relativePath == GalleryFolderIndexPolicy.ROOT
    val recentPhotos = remember(allPhotos) {
        allPhotos.filterNot { GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) }
    }
    val selectablePhotos = when {
        !root -> photos
        showRecentPhotos -> recentPhotos
        selectedTab == GalleryTab.ALL -> recentPhotos.take(9)
        selectedTab == GalleryTab.FOLDERS -> photos
        else -> allPhotos.filter { it.id in favoriteIds }
    }
    val selectableIds = selectablePhotos.mapTo(mutableSetOf()) { it.id }
    val allSelected = selectableIds.isNotEmpty() && selectedPhotoIds.containsAll(selectableIds)
    LaunchedEffect(selectableIds, showPhotoMove, photoMoveBusy, pendingPhotoPlan, deleteBusy, showPhotoDelete) {
        if (!showPhotoMove && !photoMoveBusy && pendingPhotoPlan == null && !deleteBusy && !showPhotoDelete) selectedPhotoIds = selectedPhotoIds.intersect(selectableIds)
    }
    BackHandler(enabled = showPhotoDelete && photoDeleteLoading) { showPhotoDelete = false }
    BackHandler(enabled = showRecentPhotos && !selectionActive && !showPhotoMove) { showRecentPhotos = false }
    BackHandler(enabled = selectionActive && !showPhotoMove) {
        if (!photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null) cancelSelection()
    }
    BackHandler(enabled = operationLocked && !photoDeleteLoading) { /* Preserve the active operation and its callbacks. */ }
    val title = if (showRecentPhotos) "최근 촬영" else if (root) "갤러리"
        else relativePath.trimEnd('/').substringAfterLast('/')
    val currentSummary = summariesByPath[relativePath]
    val imagesById = remember(allPhotos) { allPhotos.associateBy { it.id.toString() } }
    Column(modifier = Modifier.dzScreen().padding(horizontal = 16.dp)) {
        if (selectionActive) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = ::cancelSelection, enabled = !photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null) { Text("취소") }
                Text("${selectedPhotoIds.size}장 선택", modifier = Modifier.weight(1f),
                    color = DDZColor.TextPrimary, fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                TextButton(enabled = selectableIds.isNotEmpty() && !photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null,
                    onClick = { selectedPhotoIds = if (allSelected) emptySet() else selectableIds }) {
                    Text(if (allSelected) "전체 해제" else "전체 선택")
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(enabled = !navigationLocked, onClick = { if (!navigationLocked) {
                    if (showRecentPhotos) showRecentPhotos = false else onBack()
                } }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
                Text(title, modifier = Modifier.weight(1f), color = DDZColor.TextPrimary,
                    style = DDZTypography.ScreenTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!root) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GalleryFolderIndexPolicy.breadcrumbs(relativePath).forEachIndexed { i, crumb ->
                    if (i > 0) Text(" / ", color = DDZColor.TextSecondary)
                    Text(
                        text = crumb.label,
                        color = if (crumb.relativePath == relativePath) DDZColor.TextPrimary
                            else DDZColor.Primary,
                        modifier = Modifier.clickable(enabled = !selectionActive && !navigationLocked) {
                            if (crumb.relativePath != relativePath) onOpenFolder(crumb.relativePath)
                        }.padding(vertical = 5.dp),
                    )
                }
            }
        }
        if (!root && connected) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !navigationLocked && !selectionActive,
                    onClick = { startFolderManagement(relativePath, "rename") }) { Text("이름 변경") }
                OutlinedButton(enabled = !navigationLocked && !selectionActive,
                    onClick = { startFolderManagement(relativePath, "move") }) { Text("이동") }
                OutlinedButton(enabled = !navigationLocked && !selectionActive,
                    onClick = { startFolderManagement(relativePath, "delete") }) {
                    Text("삭제", color = DDZColor.Destructive)
                }
            }
        }
        folderOperationError?.let { message ->
            Text(message, color = DDZColor.Destructive)
        }
        if (error != null && index != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(requireNotNull(error), color = DDZColor.TextSecondary, modifier = Modifier.weight(1f))
                TextButton(onClick = { reloadKey++ }) { Text("다시 시도") }
            }
        }
        retryPhotoPlan?.let { retry ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("미완료 파일 ${retry.items.size}개", modifier = Modifier.weight(1f))
                TextButton(enabled = !photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null,
                    onClick = { showPhotoMoveRetry = true }) { Text("재시도") }
                TextButton(enabled = !photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null,
                    onClick = { retryPhotoPlan = null }) { Text("닫기") }
            }
        }
        retryDeletePlan?.let { retry ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("삭제 미완료 파일 ${retry.items.size}개", modifier = Modifier.weight(1f))
                val retryEnabled = !deleteBusy && !showPhotoDelete && !photoMoveBusy && pendingPhotoPlan == null
                TextButton(enabled = retryEnabled, onClick = { showDeleteRetry = true }) { Text("재시도") }
                TextButton(enabled = retryEnabled, onClick = { retryDeletePlan = null }) { Text("닫기") }
            }
        }
        Column(Modifier.weight(1f)) {
            if (loading && index == null) {
                GalleryLoadingSkeleton()
            } else if (error != null && index == null) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("사진 목록을 표시할 수 없습니다.", color = DDZColor.TextSecondary)
                    OutlinedButton(onClick = { reloadKey++ }) { Text("다시 시도") }
                }
            } else if (root && showRecentPhotos) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (recentPhotos.isEmpty()) {
                        item { GalleryEmptyText("저장된 사진이 없습니다.") }
                    } else {
                        items(recentPhotos.chunked(3)) { row ->
                            GalleryPhotoRow(row, selectedPhotoIds, ::enterSelection, ::togglePhoto, selectionActive = selectionActive) { photo ->
                                if (!navigationLocked) onOpenPhoto(recentPhotos, recentPhotos.indexOfFirst { it.id == photo.id })
                            }
                        }
                    }
                }
            } else if (root && index != null) {
                LogGalleryHomeContent(
                    selectedTab = selectedTab,
                    onTabChange = { if (!selectionActive && !navigationLocked) selectedTab = it },
                    folderIndex = requireNotNull(index),
                    summariesByPath = summariesByPath,
                    allImages = allPhotos,
                    favoriteIds = favoriteIds,
                    onOpenFolder = { if (!selectionActive && !navigationLocked) onOpenFolder(it) },
                    onOpenPhoto = { photos, index -> if (!navigationLocked) onOpenPhoto(photos, index) },
                    onOpenOriginal = { if (!selectionActive && !navigationLocked) onOpenOriginal(it) },
                    onOpenRecentPhotos = { if (!selectionActive && !navigationLocked) showRecentPhotos = true },
                    onManageFolder = ::startFolderManagement,
                    canManageFolders = !navigationLocked && !selectionActive && !folderAccessBusy,
                    onCreateFolder = { if (!selectionActive && !navigationLocked) { folderOperationError = null; showCreateDialog = true } },
                    selectedIds = selectedPhotoIds,
                    selectionActive = selectionActive,
                    onLongPressPhoto = ::enterSelection,
                    onSelectPhoto = ::togglePhoto,
                )
            } else {
                val current = index
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        GallerySectionTitle("하위 폴더", createEnabled = !selectionActive && !navigationLocked,
                            onCreateFolder = { folderOperationError = null; showCreateDialog = true })
                    }
                    if (current != null && current.children.isNotEmpty()) {
                        items(current.children, key = { it.relativePath }) { folder ->
                            val summary = summariesByPath[folder.relativePath]
                            GalleryFolderRow(folder, summary, summary?.coverImageId?.let(imagesById::get),
                                canManage = !navigationLocked && !selectionActive && !folderAccessBusy,
                                onManage = { action -> startFolderManagement(folder.relativePath, action) },
                            ) {
                                if (!selectionActive && !navigationLocked) onOpenFolder(folder.relativePath)
                            }
                        }
                    }
                    if (photos.isNotEmpty()) item {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("사진 ${currentSummary?.directPhotoCount ?: photos.size}장", fontWeight = FontWeight.SemiBold)
                            Box {
                                TextButton(onClick = { showPhotoSort = true }, enabled = photos.isNotEmpty()) {
                                    Text(photoSort.label)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "사진 정렬")
                                }
                                DropdownMenu(expanded = showPhotoSort, onDismissRequest = { showPhotoSort = false }) {
                                    GalleryPhotoSort.entries.forEach { sort ->
                                        DropdownMenuItem(
                                            text = { Text(if (photoSort == sort) "✓ ${sort.label}" else sort.label) },
                                            onClick = { photoSort = sort; showPhotoSort = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (photos.isNotEmpty()) {
                        items(sortedPhotos.chunked(3)) { rowPhotos ->
                            GalleryPhotoRow(rowPhotos, selectedPhotoIds, ::enterSelection, ::togglePhoto, selectionActive = selectionActive) { photo ->
                                if (!navigationLocked) onOpenPhoto(sortedPhotos, sortedPhotos.indexOfFirst { it.id == photo.id })
                            }
                        }
                    }
                    if (current != null && current.children.isEmpty() && photos.isEmpty() && current.directOriginalCount == 0) {
                        item { GalleryEmptyText("빈 폴더입니다.") }
                    }
                    if (current != null && current.directOriginalCount > 0) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable(enabled = !selectionActive && !navigationLocked) { onOpenOriginal(relativePath + "original/") }
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = DDZColor.Primary)
                                Text("원본사진 ${current.directOriginalCount}장", modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = "원본사진 열기")
                            }
                        }
                    }
                }
            }
        }
        if (photoDeleteLoading) Text("삭제 대상 확인 중…", color = DDZColor.TextSecondary)
        if (showPhotoDelete && !photoDeleteLoading && deleteSelected.isNotEmpty()) {
            GalleryPhotoDeleteConfirmation(
                selected = deleteSelected,
                allPhotos = deleteSnapshot,
                busy = deleteAction.busy,
                onCancel = { showPhotoDelete = false },
                onConfirm = { plan ->
                    showPhotoDelete = false
                    retryDeletePlan = plan
                    deleteAction.request(plan)
                },
            )
        }
        val deleteRetry = retryDeletePlan
        if (showDeleteRetry && deleteRetry != null) {
            AlertDialog(
                onDismissRequest = { if (!deleteBusy) showDeleteRetry = false },
                title = { Text("미완료 파일 삭제 확인") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("결과사진 ${deleteRetry.resultCount}장 · 원본사진 ${deleteRetry.originalCount}장")
                        Text("총 재시도 파일 ${deleteRetry.items.size}개")
                        Text("삭제한 사진은 되돌릴 수 없습니다.", color = DDZColor.Destructive)
                    }
                },
                confirmButton = {
                    TextButton(enabled = !deleteBusy && !photoMoveBusy && pendingPhotoPlan == null, onClick = {
                        showDeleteRetry = false
                        folderOperationError = null
                        deleteAction.request(deleteRetry)
                    }) { Text("삭제 재시도") }
                },
                dismissButton = {
                    TextButton(enabled = !deleteBusy, onClick = { showDeleteRetry = false }) { Text("취소") }
                },
            )
        }
        if (selectionActive) {
            val actionsEnabled = selectedPhotoIds.isNotEmpty() && !photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null
            Row(Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(vertical = 8.dp)
                .background(DDZColor.SurfaceSoft, RoundedCornerShape(16.dp))) {
                TextButton(modifier = Modifier.weight(1f), enabled = actionsEnabled, onClick = ::startPhotoMove) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DriveFileMove, contentDescription = null)
                        Text("이동")
                    }
                }
                TextButton(modifier = Modifier.weight(1f), enabled = actionsEnabled, onClick = {
                    val selected = (if (root) allPhotos else photos).filter { it.id in selectedPhotoIds }
                    runCatching { shareGalleryImages(context, selected) }.onFailure {
                        folderOperationError = it.message ?: "공유 화면을 열지 못했습니다."
                    }
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Text("공유")
                    }
                }
                TextButton(modifier = Modifier.weight(1f), enabled = actionsEnabled, onClick = ::startPhotoDelete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Text("삭제")
                    }
                }
            }
        }
        if (showPhotoMove && showMoveDestination) {
            GalleryMoveDestinationPicker(
                allPhotos = moveAllPhotos,
                knownFolderPaths = photoMoveFolders,
                selectedIds = selectedPhotoIds,
                initialPath = photoMoveTarget,
                loading = photoMoveLoading,
                loadError = photoMoveLoadError,
                onRetry = ::loadMoveDestinations,
                loadPhysicalChildren = { path -> withContext(Dispatchers.IO) { folderStorage.listImmediateFolderPaths(path) } },
                onCancel = { showPhotoMove = false; showMoveDestination = false },
                onChoose = { path ->
                    photoMoveTarget = path
                    photoMoveFolders = (photoMoveFolders + path).distinct()
                    showMoveDestination = false
                },
            )
        }
        if (showPhotoMove && !showMoveDestination) {
            GalleryPhotoMoveConfirmation(
                selected = moveAllPhotos.filter { it.id in selectedPhotoIds },
                allPhotos = moveAllPhotos,
                destinationPath = photoMoveTarget,
                busy = photoMoveBusy,
                onChangeDestination = { showMoveDestination = true },
                onCancel = { showPhotoMove = false },
                onConfirm = { prepared ->
                    showPhotoMove = false
                    requestPhotoMove(prepared)
                },
            )
        }
        val retryPlan = retryPhotoPlan
        if (showPhotoMoveRetry && retryPlan != null) {
            AlertDialog(
                onDismissRequest = { showPhotoMoveRetry = false },
                title = { Text("미완료 파일 이동 확인") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("이동 위치")
                        retryPlan.destinations.values.distinct().forEach { Text(it) }
                        val originals = retryPlan.items.count { GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) }
                        Text("결과사진 ${retryPlan.items.size - originals}장 · 원본사진 ${originals}장")
                        Text("총 재시도 파일 ${retryPlan.items.size}개")
                    }
                },
                confirmButton = {
                    TextButton(enabled = !photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null, onClick = {
                        showPhotoMoveRetry = false
                        requestPhotoMove(retryPlan)
                    }) { Text("재시도") }
                },
                dismissButton = { TextButton(onClick = { showPhotoMoveRetry = false }) { Text("취소") } },
            )
        }
        if (showFolderAccessHelp) {
            AlertDialog(
                onDismissRequest = { showFolderAccessHelp = false },
                title = { Text("DZlog 폴더 접근 연결") },
                text = { Text("빈 폴더를 확인하고 폴더 이름을 변경하거나 이동하려면 접근 권한이 필요합니다. 다음 화면에서 내장 저장공간의 Pictures/DZlog 폴더를 선택해 주세요.") },
                confirmButton = {
                    TextButton(onClick = {
                        showFolderAccessHelp = false
                        folderAccessBusy = true
                        folderOperationError = null
                        runCatching {
                            val initialUri = android.provider.DocumentsContract.buildTreeDocumentUri(
                                "com.android.externalstorage.documents", "primary:Pictures/DZlog",
                            )
                            folderAccess.launch(initialUri)
                        }.onFailure {
                            folderAccessBusy = false
                            folderOperationError = "폴더 선택 화면을 열지 못했습니다."
                        }
                    }) { Text("폴더 선택") }
                },
                dismissButton = { TextButton(onClick = { showFolderAccessHelp = false }) { Text("취소") } },
            )
        }
        if (manageAction == "delete") {
            GalleryFolderDeleteDialog(
                path = managedFolderPath,
                storage = folderStorage,
                catalog = folderCatalog,
                onCancel = { manageAction = null },
                onChanged = {
                    GallerySnapshotMemory.cache.invalidate()
                    reloadKey++
                },
                onDeleted = {
                    val deletedPath = managedFolderPath
                    manageAction = null
                    folderOperationError = "폴더 및 파일 삭제 완료"
                    if (deletedPath == relativePath) {
                        onOpenFolder(deletedPath.trimEnd('/').substringBeforeLast('/') + "/")
                    }
                },
            )
        }
        if (manageAction != null && manageAction != "delete") {
            AlertDialog(
                onDismissRequest = { if (!folderManagementBusy) manageAction = null },
                title = { Text(if (manageAction == "rename") "폴더 이름 변경" else "폴더 이동") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(managedFolderPath.removePrefix(GalleryFolderIndexPolicy.ROOT),
                            color = DDZColor.TextSecondary)
                        folderOperationError?.let { Text(it, color = DDZColor.Destructive) }
                        if (manageAction == "rename") {
                            OutlinedTextField(
                                enabled = !folderManagementBusy,
                                value = renameName,
                                onValueChange = { renameName = it },
                                label = { Text("새 폴더 이름") },
                                singleLine = true,
                            )
                        } else {
                            Text("이동할 위치를 선택하세요.")
                            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())) {
                                destinationPaths.forEach { destination ->
                                    OutlinedButton(enabled = !folderManagementBusy, onClick = { moveTarget = destination }) {
                                        val label = destination.removePrefix(GalleryFolderIndexPolicy.ROOT)
                                            .ifBlank { "DZlog" }
                                        Text(if (moveTarget == destination) "✓ $label" else label)
                                    }
                                }
                            }
                        }
                        val impact = GalleryFolderOperationPolicy.captureImpact(managedFolderPath, capturePathDrafts)
                        Text(
                            when (impact) {
                                CapturePathImpact.MATCH -> "이 폴더는 현재 저장설정의 촬영 경로입니다. 변경 후 촬영 사진은 기존 설정 경로에 저장될 수 있습니다."
                                CapturePathImpact.POSSIBLE -> "날짜·표 값 등의 가변 저장경로가 이 폴더를 사용할 수 있습니다."
                                CapturePathImpact.NONE -> "폴더 안의 사진·원본·하위 폴더가 함께 이동합니다. 촬영 저장설정은 변경하지 않습니다."
                            },
                            color = DDZColor.TextSecondary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                enabled = !folderManagementBusy,
                                checked = managementConfirmed,
                                onCheckedChange = { managementConfirmed = it },
                            )
                            Text("파일과 저장경로 영향을 확인했습니다.")
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = !folderManagementBusy && managementConfirmed && (manageAction != "move" || destinationPaths.contains(moveTarget)), onClick = {
                        val action = manageAction
                        val sourcePath = managedFolderPath
                        val requestedName = renameName
                        val destination = moveTarget
                        folderManagementBusy = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val targetPath = if (action == "rename")
                                        GalleryFolderOperationPolicy.renamedPath(sourcePath, requestedName)
                                    else GalleryFolderOperationPolicy.movedPath(sourcePath, destination)
                                    folderCatalog.relocateFolder(sourcePath, targetPath) {
                                        if (action == "rename")
                                            folderStorage.renameFolder(sourcePath, requestedName)
                                        else folderStorage.moveFolder(sourcePath, destination)
                                    }
                                }
                            }
                            folderManagementBusy = false
                            GallerySnapshotMemory.cache.invalidate()
                            result.onSuccess { newPath ->
                                manageAction = null
                                folderOperationError = null
                                if (sourcePath == relativePath) onOpenFolder(newPath)
                                reloadKey++
                            }.onFailure {
                                folderOperationError = it.message ?: "폴더 관리 실패"
                                reloadKey++
                            }
                        }
                    }) { Text("변경") }
                },
                dismissButton = {
                    TextButton(enabled = !folderManagementBusy, onClick = { manageAction = null }) { Text("취소") }
                },
            )
        }
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("새 폴더") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("폴더 이름") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val existing = reader.loadGallerySnapshot(relativePath).index.children
                                        .map { it.relativePath }
                                    folderCatalog.create(relativePath, newFolderName, existing)
                                }
                            }
                            result.onSuccess {
                                showCreateDialog = false
                                newFolderName = ""
                                folderOperationError = null
                                GallerySnapshotMemory.cache.invalidate(relativePath)
                                reloadKey++
                            }.onFailure { folderOperationError = it.message ?: "폴더 생성 실패" }
                        }
                    }) { Text("생성") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("취소") }
                },
            )
        }
    }
}

/** Sort the displayed copy; keep the MediaStore snapshot and ID-based selection intact. */
internal enum class GalleryPhotoSort(val label: String) {
    NEWEST("최신순"), OLDEST("오래된순"), NAME("이름순");

    fun sorted(photos: List<MediaImageItem>): List<MediaImageItem> = photos.sortedWith(
        when (this) {
            NEWEST -> compareByDescending<MediaImageItem> { it.dateAddedSeconds }.thenByDescending { it.id }
            OLDEST -> compareBy<MediaImageItem> { it.dateAddedSeconds }.thenBy { it.id }
            NAME -> compareBy<MediaImageItem> { it.displayName.lowercase(Locale.ROOT) }
                .thenBy { it.displayName }.thenByDescending { it.id }
        },
    )
}
