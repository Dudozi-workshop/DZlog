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
import androidx.compose.material.icons.filled.CreateNewFolder
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
import androidx.compose.ui.unit.sp
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
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummaryPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone
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
    var selectedPhotoIds by remember(relativePath) { mutableStateOf<Set<Long>>(emptySet()) }
    var selectionActive by remember(relativePath) { mutableStateOf(false) }
    var showPhotoMove by remember { mutableStateOf(false) }
    var showMoveDestination by remember { mutableStateOf(false) }
    var photoMoveLoading by remember { mutableStateOf(false) }
    var photoMoveLoadVersion by remember { mutableStateOf(0) }
    var photoMoveLoadError by remember { mutableStateOf<String?>(null) }
    var includeOriginals by remember { mutableStateOf<Boolean?>(null) }
    var photoMoveTarget by remember { mutableStateOf(GalleryFolderIndexPolicy.ROOT) }
    var photoMoveFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var photoMoveBusy by remember { mutableStateOf(false) }
    var moveAllPhotos by remember { mutableStateOf<List<MediaImageItem>>(emptyList()) }
    var pendingPhotoPlan by remember { mutableStateOf<com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan?>(null) }

    var reloadKey by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    fun finishPhotoMove(plan: com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePlan) {
        photoMoveBusy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { GalleryPhotoMover(context.contentResolver).move(plan) }
            }
            photoMoveBusy = false
            result.onSuccess { moved ->
                if (moved.moved > 0) {
                    GallerySnapshotMemory.cache.invalidate()
                    selectedPhotoIds = emptySet()
                    selectionActive = false
                    reloadKey++
                }
                folderOperationError = if (moved.failed > 0) {
                    "이동 ${moved.moved}장, 미완료 ${moved.failed}장. ${moved.detail.orEmpty()}"
                } else "사진 ${moved.moved}장 이동 완료"
            }.onFailure { folderOperationError = it.message ?: "사진 이동 실패" }
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        connected = folderStorage.isConnected()
        reloadKey++
    }

    LaunchedEffect(reader, relativePath, reloadKey) {
        loading = index == null
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val physicalFolders = if (folderStorage.isConnected())
                    runCatching { folderStorage.listImmediateFolderPaths(relativePath) }.getOrDefault(emptyList())
                    else emptyList()
                // Each visible child needs its immediate directories for its summary.
                // A bounded second level is enough; no recursive SAF tree scan is needed.
                val physicalChildFolders = physicalFolders
                    .filterNot { it.trimEnd('/').substringAfterLast('/').equals("original", true) }
                    .flatMap { childPath ->
                        runCatching { folderStorage.listImmediateFolderPaths(childPath) }
                            .getOrDefault(emptyList())
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
        }.onFailure { if (index == null) error = "목록을 불러오지 못했습니다." }
        loading = false
    }

    fun enterSelection(photo: MediaImageItem) {
        selectionActive = true
        selectedPhotoIds = selectedPhotoIds + photo.id
    }
    fun cancelSelection() {
        selectedPhotoIds = emptySet()
        selectionActive = false
    }
    fun togglePhoto(photo: MediaImageItem) {
        selectionActive = true
        selectedPhotoIds = if (photo.id in selectedPhotoIds) selectedPhotoIds - photo.id else selectedPhotoIds + photo.id
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
        if (selectedPhotoIds.isEmpty() || photoMoveBusy || pendingPhotoPlan != null) return
        folderOperationError = null
        showPhotoMove = true
        showMoveDestination = true
        includeOriginals = null
        photoMoveTarget = GalleryFolderIndexPolicy.ROOT
        moveAllPhotos = emptyList()
        photoMoveFolders = emptyList()
        loadMoveDestinations()
    }
    fun startFolderManagement(path: String, action: String) {
        if (folderManagementBusy || selectionActive) return
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
    LaunchedEffect(selectableIds, showPhotoMove, photoMoveBusy) {
        if (!showPhotoMove && !photoMoveBusy) selectedPhotoIds = selectedPhotoIds.intersect(selectableIds)
    }
    BackHandler(enabled = showRecentPhotos && !selectionActive && !showPhotoMove) { showRecentPhotos = false }
    BackHandler(enabled = selectionActive && !showPhotoMove) {
        if (!photoMoveBusy && pendingPhotoPlan == null) cancelSelection()
    }
    val title = if (showRecentPhotos) "최근 촬영" else if (root) "갤러리"
        else relativePath.trimEnd('/').substringAfterLast('/')
    val currentSummary = summariesByPath[relativePath]
    val imagesById = remember(allPhotos) { allPhotos.associateBy { it.id.toString() } }
    Column(modifier = Modifier.dzScreen().padding(horizontal = 16.dp)) {
        if (selectionActive) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = ::cancelSelection, enabled = !photoMoveBusy && pendingPhotoPlan == null) { Text("취소") }
                Text("${selectedPhotoIds.size}장 선택", modifier = Modifier.weight(1f),
                    color = DDZColor.TextPrimary, fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                TextButton(enabled = selectableIds.isNotEmpty() && !photoMoveBusy && pendingPhotoPlan == null,
                    onClick = { selectedPhotoIds = if (allSelected) emptySet() else selectableIds }) {
                    Text(if (allSelected) "전체 해제" else "전체 선택")
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { if (showRecentPhotos) showRecentPhotos = false else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = DDZColor.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(if (showRecentPhotos) "사진 ${recentPhotos.size}장" else
                        relativePath.removePrefix(GalleryFolderIndexPolicy.ROOT).ifBlank { "Pictures / DZlog" },
                        color = DDZColor.TextSecondary, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    if (!root && currentSummary != null) {
                        Text(
                            "사진 ${currentSummary.totalPhotoCount} · 하위 ${currentSummary.directChildFolderCount} · 최근 " +
                                GalleryFolderSummaryPolicy.compactDate(
                                    epochMillis = currentSummary.latestPhotoEpochMillis,
                                    referenceMillis = System.currentTimeMillis(),
                                    timeZone = TimeZone.getDefault(),
                                ),
                            color = DDZColor.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
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
                        modifier = Modifier.clickable(enabled = !selectionActive) {
                            if (crumb.relativePath != relativePath) onOpenFolder(crumb.relativePath)
                        }.padding(vertical = 5.dp),
                    )
                }
            }
        }
        if (!root && connected) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !folderManagementBusy && !selectionActive,
                    onClick = { startFolderManagement(relativePath, "rename") }) { Text("이름 변경") }
                OutlinedButton(enabled = !folderManagementBusy && !selectionActive,
                    onClick = { startFolderManagement(relativePath, "move") }) { Text("이동") }
            }
        }
        folderOperationError?.let { message ->
            Text(message, color = DDZColor.Destructive)
        }
        Column(Modifier.weight(1f)) {
            if (loading && index == null) {
                GalleryLoadingSkeleton()
            } else if (error != null) {
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
                                onOpenPhoto(recentPhotos, recentPhotos.indexOfFirst { it.id == photo.id })
                            }
                        }
                    }
                }
            } else if (root && index != null) {
                LogGalleryHomeContent(
                    selectedTab = selectedTab,
                    onTabChange = { if (!selectionActive) selectedTab = it },
                    folderIndex = requireNotNull(index),
                    summariesByPath = summariesByPath,
                    allImages = allPhotos,
                    favoriteIds = favoriteIds,
                    onOpenFolder = { if (!selectionActive) onOpenFolder(it) },
                    onOpenPhoto = onOpenPhoto,
                    onOpenOriginal = { if (!selectionActive) onOpenOriginal(it) },
                    onOpenRecentPhotos = { if (!selectionActive) showRecentPhotos = true },
                    onManageFolder = ::startFolderManagement,
                    canManageFolders = connected && !folderManagementBusy && !selectionActive,
                    onCreateFolder = { folderOperationError = null; showCreateDialog = true },
                    selectedIds = selectedPhotoIds,
                    selectionActive = selectionActive,
                    onLongPressPhoto = ::enterSelection,
                    onSelectPhoto = ::togglePhoto,
                )
            } else {
                val current = index
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("하위 폴더", fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { folderOperationError = null; showCreateDialog = true }) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                Text(" 새 폴더")
                            }
                        }
                    }
                    if (current != null && current.children.isNotEmpty()) {
                        items(current.children, key = { it.relativePath }) { folder ->
                            val summary = summariesByPath[folder.relativePath]
                            GalleryFolderRow(folder, summary, summary?.coverImageId?.let(imagesById::get),
                                canManage = connected && !folderManagementBusy && !selectionActive,
                                onManage = { action -> startFolderManagement(folder.relativePath, action) },
                            ) {
                                if (!selectionActive) onOpenFolder(folder.relativePath)
                            }
                        }
                    }
                    item {
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
                    if (photos.isEmpty()) {
                        item { Text("이 폴더에 저장된 사진이 없습니다.", color = DDZColor.TextSecondary) }
                    } else {
                        items(sortedPhotos.chunked(3)) { rowPhotos ->
                            GalleryPhotoRow(rowPhotos, selectedPhotoIds, ::enterSelection, ::togglePhoto, selectionActive = selectionActive) { photo ->
                                onOpenPhoto(sortedPhotos, sortedPhotos.indexOfFirst { it.id == photo.id })
                            }
                        }
                    }
                    if (current != null && current.directOriginalCount > 0) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable(enabled = !selectionActive) { onOpenOriginal(relativePath + "original/") }
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
        if (selectionActive) {
            val actionsEnabled = selectedPhotoIds.isNotEmpty() && !photoMoveBusy && pendingPhotoPlan == null
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
                // Deletion stays unavailable until its confirmation and permission flow is implemented.
                TextButton(modifier = Modifier.weight(1f), enabled = false, onClick = {}) {
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
            val selected = moveAllPhotos.filter { it.id in selectedPhotoIds }
            val originalMatches = GalleryPhotoMovePolicy.pairedOriginals(selected, moveAllPhotos)
            AlertDialog(
                onDismissRequest = { if (!photoMoveBusy) showPhotoMove = false },
                title = { Text("사진 이동") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        folderOperationError?.let { Text(it, color = DDZColor.Destructive) }
                        Text("선택한 사진 ${selected.size}장")
                        Text("이동 위치")
                        Text(photoMoveTarget.removePrefix(GalleryFolderIndexPolicy.ROOT).ifBlank { "DZlog" })
                        TextButton(onClick = { showMoveDestination = true }) { Text("위치 다시 선택") }
                        if (originalMatches.isNotEmpty()) {
                            Text("연결 후보 원본 ${originalMatches.size}장도 함께 이동할까요?")
                            Row {
                                OutlinedButton(onClick = { includeOriginals = false }) {
                                    Text(if (includeOriginals == false) "✓ 사진만" else "사진만")
                                }
                                OutlinedButton(onClick = { includeOriginals = true }) {
                                    Text(if (includeOriginals == true) "✓ 원본 포함" else "원본 포함")
                                }
                            }
                        } else {
                            Text("연결된 원본사진 없음")
                        }
                        Text("촬영 저장규칙은 변경되지 않습니다.", color = DDZColor.TextSecondary)
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !photoMoveBusy && photoMoveFolders.contains(photoMoveTarget) &&
                            (originalMatches.isEmpty() || includeOriginals != null),
                        onClick = {
                            val plan = runCatching {
                                GalleryPhotoMovePolicy.plan(
                                    selected, originalMatches, photoMoveTarget, includeOriginals == true,
                                )
                            }
                            plan.onSuccess { prepared ->
                                showPhotoMove = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    runCatching {
                                        pendingPhotoPlan = prepared
                                        val request = MediaStore.createWriteRequest(
                                            context.contentResolver, prepared.items.map { it.uri },
                                        )
                                        photoWritePermission.launch(
                                            IntentSenderRequest.Builder(request.intentSender).build()
                                        )
                                    }.onFailure {
                                        pendingPhotoPlan = null
                                        folderOperationError = "사진 수정 권한 요청을 열지 못했습니다."
                                    }
                                } else {
                                    finishPhotoMove(prepared)
                                }
                            }.onFailure { folderOperationError = it.message ?: "사진 이동 준비 실패" }
                        },
                    ) { Text(if (photoMoveBusy) "이동 중" else "이동") }
                },
                dismissButton = { TextButton(onClick = { showPhotoMove = false }) { Text("취소") } },
            )
        }
        if (manageAction != null) {
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
