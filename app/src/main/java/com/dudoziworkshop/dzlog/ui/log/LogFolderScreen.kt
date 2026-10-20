package com.dudoziworkshop.dzlog.ui.log

import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialLocation
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialTarget
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import com.dudoziworkshop.dzlog.feature.help.ui.tutorialTarget
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog as AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.DisposableEffect
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
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryDefaultStoragePolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.domain.gallery.GalleryGridColumnsPolicy
import com.dudoziworkshop.dzlog.domain.gallery.GalleryPhotoRowAnchorPolicy
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
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    onOpenOriginal: (String, Boolean) -> Unit,
    showDefaultPhotos: Boolean = false,
    onShowDefaultPhotosChange: (Boolean) -> Unit = {},
    capturePathDrafts: List<List<TableEditorSlotDraft?>> = emptyList(),
    gridColumns: Int = 3,
    showCounterBadges: Boolean = true,
    onGridColumnsChange: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val displayedGridColumns = GalleryGridColumnsPolicy.normalize(gridColumns)
    val photoListState = rememberLazyListState()
    var pendingRowAnchor by remember {
        mutableStateOf<Pair<Int, GalleryPhotoRowAnchorPolicy.Position>?>(null)
    }
    LaunchedEffect(displayedGridColumns, pendingRowAnchor) {
        pendingRowAnchor?.let { pending ->
            if (displayedGridColumns == pending.first) {
                photoListState.scrollToItem(pending.second.itemIndex, pending.second.scrollOffset)
                pendingRowAnchor = null
            }
        }
    }
    var lastColumnHint by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(lastColumnHint) {
        if (lastColumnHint != null) {
            delay(850)
            lastColumnHint = null
        }
    }
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
    var directPhotos by remember(relativePath) { mutableStateOf(cached?.directPhotos ?: emptyList()) }
    val originalsInPlace = GalleryFolderIndexPolicy.showsOriginalsDirectly(index)
    val photos = remember(directPhotos, allPhotos, originalsInPlace, relativePath) {
        if (originalsInPlace) allPhotos.filter { it.relativePath == relativePath + "original/" }
        else directPhotos
    }
    var photoSort by rememberSaveable(relativePath) { mutableStateOf(GalleryPhotoSort.NEWEST) }
    var favoriteOnly by rememberSaveable(relativePath) { mutableStateOf(false) }
    var showPhotoSort by remember(relativePath) { mutableStateOf(false) }
    val visiblePhotos = remember(photos, favoriteOnly, favoriteIds) {
        if (favoriteOnly) photos.filter { it.id in favoriteIds } else photos
    }
    val sortedPhotos = remember(visiblePhotos, photoSort) { photoSort.sorted(visiblePhotos) }
    val originalGrid = GalleryPhotoMovePolicy.isOriginalPath(relativePath) || originalsInPlace
    val displayFolderPath = if (GalleryPhotoMovePolicy.isOriginalPath(relativePath)) GalleryFolderIndexPolicy.parentOf(relativePath)
        ?: GalleryFolderIndexPolicy.ROOT else relativePath
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
    var showFolderMovePicker by remember { mutableStateOf(false) }
    var folderMoveHasChoice by remember { mutableStateOf(false) }
    var folderMoveLoading by remember { mutableStateOf(false) }
    var folderMoveLoadError by remember { mutableStateOf<String?>(null) }
    var folderMoveLoadVersion by remember { mutableStateOf(0) }
    var folderMovePhotos by remember { mutableStateOf<List<MediaImageItem>>(emptyList()) }
    var folderOperationError by remember { mutableStateOf<String?>(null) }
    var connected by remember { mutableStateOf(folderStorage.isConnected()) }
    var showFolderAccessHelp by rememberSaveable { mutableStateOf(false) }
    var folderAccessBusy by rememberSaveable { mutableStateOf(false) }
    var pendingFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFolderAction by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAccessPath by rememberSaveable { mutableStateOf<String?>(null) }
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
    val tourLocation = LocalTutorialLocation.current
    val readOnlyTour = LocalTutorialReadOnly.current
    LaunchedEffect(tourLocation, allPhotos, index) {
        if (tourLocation?.areaId == "gallery" && tourLocation.sessionId == "photos") {
            showRecentPhotos = true
            selectedPhotoIds = allPhotos.firstOrNull()?.let { setOf(it.id) } ?: emptySet()
            selectionActive = true
        } else if (tourLocation != null) {
            selectionActive = false
            selectedPhotoIds = emptySet()
            showRecentPhotos = false
        }
        if (tourLocation?.areaId == "gallery") {
            showCreateDialog = tourLocation.pageId == "folders/new"
            if (showCreateDialog) newFolderName = "안내 예시 · 생성하지 않음"
            val exampleFolder = index?.children?.firstOrNull()?.relativePath
            manageAction = if (exampleFolder != null) when (tourLocation.pageId) {
                "folders/rename" -> "rename"
                "folders/move" -> "move"
                "folders/delete" -> "delete"
                else -> null
            } else null
            if (exampleFolder != null) {
                managedFolderPath = exampleFolder
                renameName = exampleFolder.trimEnd('/').substringAfterLast('/')
                destinationPaths = listOf(GalleryFolderIndexPolicy.ROOT)
                moveTarget = GalleryFolderIndexPolicy.ROOT
            }
        }
    }
    val deleteBusy = deleteAction.busy || photoDeleteLoading
    val operationLocked = deleteBusy || photoMoveBusy || pendingPhotoPlan != null || folderManagementBusy
    val navigationLocked = operationLocked || manageAction != null || showPhotoDelete || showPhotoMove || folderAccessBusy || showFolderAccessHelp
    val folderAccess = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri == null) {
            folderAccessBusy = false
            pendingFolderAction = null
            pendingFolderPath = null
            selectedAccessPath = null
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
                    selectedAccessPath = runCatching {
                        android.provider.DocumentsContract.getTreeDocumentId(treeUri).substringAfter(':')
                    }.getOrDefault("선택한 경로를 확인할 수 없어요")
                    showFolderAccessHelp = true
                    folderOperationError = null
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

    DisposableEffect(context) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                GallerySnapshotMemory.cache.invalidate()
                reloadKey++
            }
        }
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
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
            directPhotos = snapshot.directPhotos
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
    fun loadFolderMoveDestinations(sourcePath: String) {
        val requestVersion = ++folderMoveLoadVersion
        folderMoveLoading = true
        folderMoveLoadError = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    folderStorage.listDestinationFolders(sourcePath) to
                        reader.loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT)
                }
            }
            if (requestVersion != folderMoveLoadVersion ||
                managedFolderPath != sourcePath || manageAction != "move") return@launch
            result.onSuccess { (paths, images) ->
                destinationPaths = paths
                folderMovePhotos = images
            }.onFailure {
                folderMoveLoadError = it.message ?: "폴더 목록을 불러오지 못했습니다."
            }
            folderMoveLoading = false
        }
    }
    fun startFolderManagement(path: String, action: String) {
        if (folderManagementBusy || folderAccessBusy || selectionActive || manageAction != null || deleteBusy || photoMoveBusy) return
        connected = folderStorage.isConnected()
        if (!connected) {
            pendingFolderPath = path
            pendingFolderAction = action
            selectedAccessPath = null
            showFolderAccessHelp = true
            return
        }
        managedFolderPath = path
        folderOperationError = null
        renameName = path.trimEnd('/').substringAfterLast('/')
        moveTarget = GalleryFolderIndexPolicy.ROOT
        destinationPaths = emptyList()
        folderMovePhotos = emptyList()
        folderMoveHasChoice = false
        showFolderMovePicker = action == "move"
        manageAction = action
        if (action == "move") loadFolderMoveDestinations(path)
    }
    LaunchedEffect(connected, folderAccessBusy, showFolderAccessHelp, pendingFolderPath, pendingFolderAction) {
        val pending = pendingFolderAction
        val pendingPath = pendingFolderPath
        if (connected && !folderAccessBusy && !showFolderAccessHelp && pending != null && pendingPath != null) {
            pendingFolderAction = null
            pendingFolderPath = null
            startFolderManagement(pendingPath, pending)
        }
    }
    val root = relativePath == GalleryFolderIndexPolicy.ROOT
    val recentPhotos = remember(allPhotos) { GalleryPhotoSort.NEWEST.sorted(allPhotos) }
    val defaultPhotos = remember(allPhotos) {
        allPhotos.filter { GalleryDefaultStoragePolicy.isDirectRootPhoto(it.relativePath) }
    }
    val sortedDefaultPhotos = remember(defaultPhotos, photoSort) { photoSort.sorted(defaultPhotos) }
    val selectablePhotos = when {
        !root -> visiblePhotos
        showDefaultPhotos -> defaultPhotos
        showRecentPhotos -> recentPhotos
        selectedTab == GalleryTab.ALL -> recentPhotos.take(9)
        else -> allPhotos.filter { it.id in favoriteIds }
    }
    val selectableIds = selectablePhotos.mapTo(mutableSetOf()) { it.id }
    val allSelected = selectableIds.isNotEmpty() && selectedPhotoIds.containsAll(selectableIds)
    LaunchedEffect(selectableIds, showPhotoMove, photoMoveBusy, pendingPhotoPlan, deleteBusy, showPhotoDelete) {
        if (!showPhotoMove && !photoMoveBusy && pendingPhotoPlan == null && !deleteBusy && !showPhotoDelete) selectedPhotoIds = selectedPhotoIds.intersect(selectableIds)
    }
    BackHandler(enabled = showPhotoDelete && photoDeleteLoading) { showPhotoDelete = false }
    BackHandler(enabled = showRecentPhotos && !selectionActive && !showPhotoMove) { showRecentPhotos = false }
    BackHandler(enabled = showDefaultPhotos && !selectionActive && !showPhotoMove) { onShowDefaultPhotosChange(false) }
    BackHandler(enabled = selectionActive && !showPhotoMove) {
        if (!photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null) cancelSelection()
    }
    BackHandler(enabled = operationLocked && !photoDeleteLoading) { /* Preserve the active operation and its callbacks. */ }
    val photoListPinchModifier = Modifier.galleryGridPinch(
        columns = displayedGridColumns,
        enabled = !selectionActive && !navigationLocked,
    ) { nextColumns ->
        val photoCount: Int
        val firstPhotoRow: Int
        when {
            root && showRecentPhotos -> {
                photoCount = recentPhotos.size
                firstPhotoRow = 0
            }
            root && showDefaultPhotos -> {
                photoCount = sortedDefaultPhotos.size
                firstPhotoRow = 1 + if ((index?.directOriginalCount ?: 0) > 0) 1 else 0
            }
            else -> {
                photoCount = sortedPhotos.size
                firstPhotoRow =
                    (if (!originalGrid && (index?.directOriginalCount ?: 0) > 0) 1 else 0) +
                    (if (!originalGrid) 1 else 0) +
                    (index?.children?.size ?: 0) +
                    (if (visiblePhotos.isNotEmpty()) 1 else 0)
            }
        }
        pendingRowAnchor = nextColumns to GalleryPhotoRowAnchorPolicy.remap(
            visibleItemIndex = photoListState.firstVisibleItemIndex,
            visibleScrollOffset = photoListState.firstVisibleItemScrollOffset,
            firstPhotoRowIndex = firstPhotoRow,
            photoCount = photoCount,
            oldColumns = displayedGridColumns,
            newColumns = nextColumns,
        )
        lastColumnHint = nextColumns
        onGridColumnsChange(nextColumns)
    }
    val title = if (showRecentPhotos) "최근 촬영" else if (showDefaultPhotos) "기본 저장"
        else if (displayFolderPath == GalleryFolderIndexPolicy.ROOT) "갤러리"
        else displayFolderPath.trimEnd('/').substringAfterLast('/')
    val imagesById = remember(allPhotos) { allPhotos.associateBy { it.id.toString() } }
    Box(Modifier.fillMaxSize()) {
    Column(modifier = Modifier.dzScreen()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        .padding(horizontal = 16.dp)) {
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
                    when {
                        showRecentPhotos -> showRecentPhotos = false
                        showDefaultPhotos -> onShowDefaultPhotosChange(false)
                        else -> onBack()
                    }
                } }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
                Text(title, modifier = Modifier.weight(1f), color = DDZColor.TextPrimary,
                    style = DDZTypography.ScreenTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                lastColumnHint?.let { newColumns ->
                    Text("${newColumns}열", style = DDZTypography.Caption, color = DDZColor.Primary)
                }
                if (originalGrid) {
                    Text("원본", style = DDZTypography.Caption, color = DDZColor.Primary,
                        modifier = Modifier.background(DDZColor.SageLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp))
                }
                if (!root) IconButton(enabled = !navigationLocked,
                    onClick = { favoriteOnly = !favoriteOnly }) {
                    Icon(if (favoriteOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (favoriteOnly) "전체 사진 보기" else "즐겨찾기만 보기",
                        tint = DDZColor.Primary)
                }
            }
        }
        if (!root) {
            Row(
                modifier = Modifier.fillMaxWidth().tutorialTarget("gallery.breadcrumbs")
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GalleryFolderIndexPolicy.breadcrumbs(displayFolderPath).forEachIndexed { i, crumb ->
                    if (i > 0) Text(" / ", color = DDZColor.TextSecondary)
                    Text(
                        text = crumb.label,
                        color = if (crumb.relativePath == displayFolderPath) DDZColor.TextPrimary
                            else DDZColor.Primary,
                        modifier = Modifier.clickable(enabled = !selectionActive && !navigationLocked) {
                            if (crumb.relativePath != relativePath) onOpenFolder(crumb.relativePath)
                        }.padding(vertical = 5.dp),
                    )
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
        Column(Modifier.weight(1f).tutorialTarget(
            if (readOnlyTour && tourLocation?.pageId in setOf("folders/rename", "folders/move", "folders/delete") && index?.children.isNullOrEmpty())
                "guide.dialog" else "gallery.content")) {
            if (loading && index == null) {
                GalleryLoadingSkeleton()
            } else if (error != null && index == null) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("사진 목록을 표시할 수 없습니다.", color = DDZColor.TextSecondary)
                    OutlinedButton(onClick = { reloadKey++ }) { Text("다시 시도") }
                }
            } else if (root && showRecentPhotos) {
                LazyColumn(modifier = photoListPinchModifier, state = photoListState,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)) {
                    if (recentPhotos.isEmpty()) {
                        item { GalleryEmptyText("저장된 사진이 없습니다.") }
                    } else {
                        items(recentPhotos.chunked(displayedGridColumns)) { row ->
                            GalleryPhotoRow(row, selectedPhotoIds, ::enterSelection, ::togglePhoto, selectionActive = selectionActive, columns = displayedGridColumns, showCounterBadges = showCounterBadges) { photo ->
                                if (!navigationLocked) onOpenPhoto(recentPhotos, recentPhotos.indexOfFirst { it.id == photo.id })
                            }
                        }
                    }
                }
            } else if (root && showDefaultPhotos) {
                LazyColumn(modifier = photoListPinchModifier, state = photoListState,
                    verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap),
                    contentPadding = PaddingValues(bottom = DDZSpacing.sectionGap)) {
                    item {
                        Row(Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("기본 저장 사진 ${defaultPhotos.size}장", style = DDZTypography.CompactSectionTitle)
                            Box {
                                TextButton(onClick = { showPhotoSort = true }, enabled = !navigationLocked && !selectionActive) {
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
                    if ((index?.directOriginalCount ?: 0) > 0) {
                        item(key = "default-originals") {
                            GalleryOriginalRow(requireNotNull(index).directOriginalCount) {
                                if (!selectionActive && !navigationLocked) onOpenOriginal(GalleryFolderIndexPolicy.ROOT + "original/", defaultPhotos.isNotEmpty())
                            }
                        }
                    }
                    if (defaultPhotos.isEmpty() && (index?.directOriginalCount ?: 0) == 0) {
                        item { GalleryEmptyText("기본 저장 사진이 없습니다.") }
                    } else {
                        if (defaultPhotos.isNotEmpty()) items(sortedDefaultPhotos.chunked(displayedGridColumns)) { row ->
                            GalleryPhotoRow(row, selectedPhotoIds, ::enterSelection, ::togglePhoto, selectionActive = selectionActive, columns = displayedGridColumns, showCounterBadges = showCounterBadges) { photo ->
                                if (!navigationLocked) onOpenPhoto(sortedDefaultPhotos, sortedDefaultPhotos.indexOfFirst { it.id == photo.id })
                            }
                        }
                    }

                }
            } else if (root && index != null) {
                LogGalleryHomeContent(
                    gridColumns = displayedGridColumns,
                    showCounterBadges = showCounterBadges,
                    onGridColumnsChange = { nextColumns ->
                        lastColumnHint = nextColumns
                        onGridColumnsChange(nextColumns)
                    },
                    selectedTab = selectedTab,
                    onTabChange = { if (!selectionActive && !navigationLocked) selectedTab = it },
                    folderIndex = requireNotNull(index),
                    summariesByPath = summariesByPath,
                    allImages = allPhotos,
                    favoriteIds = favoriteIds,
                    onOpenFolder = { if (!selectionActive && !navigationLocked) onOpenFolder(it) },
                    onOpenPhoto = { photos, index -> if (!navigationLocked) onOpenPhoto(photos, index) },
                    onOpenOriginal = { originalPath ->
                        if (!selectionActive && !navigationLocked) {
                            // Keep the virtual default-storage context when entering its original folder.
                            onShowDefaultPhotosChange(true)
                            onOpenOriginal(originalPath, defaultPhotos.isNotEmpty())
                        }
                    },
                    onOpenRecentPhotos = { if (!selectionActive && !navigationLocked) { onShowDefaultPhotosChange(false); showRecentPhotos = true } },
                    onOpenDefaultPhotos = { if (!selectionActive && !navigationLocked) { showRecentPhotos = false; onShowDefaultPhotosChange(true) } },
                    onSelectDefaultPhotos = { if (!navigationLocked && !selectionActive) { showRecentPhotos = false; onShowDefaultPhotosChange(true); selectionActive = true } },
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
                LazyColumn(modifier = photoListPinchModifier, state = photoListState,
                    verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap),
                    contentPadding = PaddingValues(bottom = DDZSpacing.sectionGap)) {
                    if (!originalGrid && current != null && current.directOriginalCount > 0) {
                        item(key = "folder-originals") {
                            GalleryOriginalRow(current.directOriginalCount) {
                                if (!selectionActive && !navigationLocked) onOpenOriginal(relativePath + "original/", directPhotos.isNotEmpty())
                            }
                        }
                    }
                    if (!originalGrid) item {
                        GallerySectionTitle(
                            title = "하위 폴더",
                            actionLabel = "새 폴더",
                            onAction = { folderOperationError = null; showCreateDialog = true },
                            actionEnabled = !selectionActive && !navigationLocked,
                            showAddIcon = true,
                        )
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
                    if (visiblePhotos.isNotEmpty()) item {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${if (originalGrid) "원본사진" else "사진"} ${visiblePhotos.size}장",
                                modifier = Modifier.weight(1f), style = DDZTypography.CompactSectionTitle)
                            Box {
                                TextButton(onClick = { showPhotoSort = true }, enabled = !navigationLocked && !selectionActive) {
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
                            TextButton(enabled = !navigationLocked && !selectionActive && sortedPhotos.isNotEmpty(),
                                onClick = { selectionActive = true }) { Text("선택") }
                        }
                    }
                    if (favoriteOnly && photos.isNotEmpty() && visiblePhotos.isEmpty()) {
                        item { GalleryEmptyText("즐겨찾기한 사진이 없습니다.") }
                    } else if (visiblePhotos.isNotEmpty()) {
                        items(sortedPhotos.chunked(displayedGridColumns)) { rowPhotos ->
                            GalleryPhotoRow(rowPhotos, selectedPhotoIds, ::enterSelection, ::togglePhoto, selectionActive = selectionActive, columns = displayedGridColumns, showCounterBadges = showCounterBadges) { photo ->
                                if (!navigationLocked) onOpenPhoto(sortedPhotos, sortedPhotos.indexOfFirst { it.id == photo.id })
                            }
                        }
                    }
                    if (current != null && current.children.isEmpty() && photos.isEmpty() && current.directOriginalCount == 0) {
                        item { GalleryEmptyText("빈 폴더입니다.") }
                    }

                }
            }
        }
        if (photoDeleteLoading) Text("삭제 대상 확인 중…", color = DDZColor.TextSecondary)
        deleteAction.progressText?.let { progress ->
            Text(progress, color = DDZColor.TextSecondary, style = DDZTypography.Caption)
        }
        if (selectionActive) {
            val actionsEnabled = selectedPhotoIds.isNotEmpty() && !photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null
            Row(Modifier.fillMaxWidth().tutorialTarget("gallery.selection-actions")
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
    }
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
                    DDZDialogAction(style = DDZButtonStyle.Destructive, enabled = !deleteBusy && !photoMoveBusy && pendingPhotoPlan == null, onClick = {
                        showDeleteRetry = false
                        folderOperationError = null
                        deleteAction.request(deleteRetry)
                    }) { Text("삭제 재시도") }
                },
                dismissButton = {
                    DDZDialogAction(style = DDZButtonStyle.Secondary, enabled = !deleteBusy, onClick = { showDeleteRetry = false }) { Text("취소") }
                },
            )
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
                    DDZDialogAction(style = DDZButtonStyle.Primary, enabled = !photoMoveBusy && !deleteBusy && !showPhotoDelete && pendingPhotoPlan == null, onClick = {
                        showPhotoMoveRetry = false
                        requestPhotoMove(retryPlan)
                    }) { Text("재시도") }
                },
                dismissButton = { DDZDialogAction(style = DDZButtonStyle.Secondary, onClick = { showPhotoMoveRetry = false }) { Text("취소") } },
            )
        }
        if (showFolderAccessHelp) {
            GalleryFolderAccessGuide(
                selectedPath = selectedAccessPath,
                onCancel = {
                    showFolderAccessHelp = false
                    pendingFolderAction = null
                    pendingFolderPath = null
                    selectedAccessPath = null
                },
                onSelect = {
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
                        pendingFolderAction = null
                    pendingFolderPath = null
                        folderOperationError = "폴더 선택 화면을 열지 못했습니다."
                    }
                },
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
                    folderOperationError = null
                    android.widget.Toast.makeText(context, "폴더가 삭제되었어요", android.widget.Toast.LENGTH_SHORT).show()
                    if (deletedPath == relativePath) {
                        onOpenFolder(deletedPath.trimEnd('/').substringBeforeLast('/') + "/")
                    }
                },
            )
        }
        if (manageAction == "rename") {
            GalleryFolderManagementDialog(
                action = requireNotNull(manageAction),
                sourcePath = managedFolderPath,
                renameName = renameName,
                destinationPath = moveTarget,
                onRenameNameChange = { renameName = it },
                impact = GalleryFolderOperationPolicy.captureImpact(managedFolderPath, capturePathDrafts),
                busy = folderManagementBusy,
                error = folderOperationError,
                onCancel = { manageAction = null },
                onConfirm = {
                    if (readOnlyTour) return@GalleryFolderManagementDialog
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
                },
            )
        }
        if (manageAction == "move" && showFolderMovePicker) {
            GalleryMoveDestinationPicker(
                allPhotos = folderMovePhotos,
                knownFolderPaths = destinationPaths,
                selectedIds = emptySet(),
                initialPath = if (folderMoveHasChoice) moveTarget else GalleryFolderIndexPolicy.ROOT,
                loading = folderMoveLoading,
                loadError = folderMoveLoadError,
                onRetry = { loadFolderMoveDestinations(managedFolderPath) },
                loadPhysicalChildren = { path ->
                    withContext(Dispatchers.IO) { folderStorage.listImmediateFolderPaths(path) }
                },
                onCancel = {
                    if (folderMoveHasChoice) showFolderMovePicker = false
                    else manageAction = null
                },
                onChoose = { selectedPath ->
                    val valid = selectedPath in destinationPaths &&
                        runCatching {
                            GalleryFolderOperationPolicy.validateMove(managedFolderPath, selectedPath)
                        }.isSuccess
                    if (valid) {
                        moveTarget = selectedPath
                        folderMoveHasChoice = true
                        folderOperationError = null
                        showFolderMovePicker = false
                    } else {
                        folderMoveLoadError = "이 위치로는 폴더를 이동할 수 없습니다."
                    }
                },
                movingFolderPath = managedFolderPath,
            )
        }
        if (manageAction == "move" && !showFolderMovePicker) {
            GalleryFolderManagementDialog(
                action = "move",
                sourcePath = managedFolderPath,
                renameName = renameName,
                destinationPath = moveTarget,
                onRenameNameChange = { renameName = it },
                impact = GalleryFolderOperationPolicy.captureImpact(managedFolderPath, capturePathDrafts),
                busy = folderManagementBusy,
                error = folderOperationError,
                onCancel = { manageAction = null },
                onConfirm = {
                    if (readOnlyTour) return@GalleryFolderManagementDialog
                    if (!folderManagementBusy && moveTarget in destinationPaths) {
                        val sourcePath = managedFolderPath
                        val destination = moveTarget
                        folderManagementBusy = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val targetPath = GalleryFolderOperationPolicy.movedPath(sourcePath, destination)
                                    folderCatalog.relocateFolder(sourcePath, targetPath) {
                                        folderStorage.moveFolder(sourcePath, destination)
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
                                folderOperationError = it.message ?: "폴더 이동 실패"
                                reloadKey++
                            }
                        }
                    }
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
                    DDZDialogAction(style = DDZButtonStyle.Primary, onClick = {
                        if (readOnlyTour) return@DDZDialogAction
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
                    DDZDialogAction(style = DDZButtonStyle.Secondary, onClick = { showCreateDialog = false }) { Text("취소") }
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
