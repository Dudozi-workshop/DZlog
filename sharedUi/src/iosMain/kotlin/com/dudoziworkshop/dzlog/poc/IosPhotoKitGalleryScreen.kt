package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.*
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.jetbrains.skia.Image as SkiaImage
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.create

private const val GALLERY_ROOT = "Pictures/DZlog/"

/** Browses the app's indexed Photos assets; logical folders never rename Photos resources. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IosPhotoKitGalleryScreen(repository: PhotoKitCaptureMediaRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var photos by remember { mutableStateOf<List<SavedCaptureMedia>>(emptyList()) }
    var folders by remember { mutableStateOf<List<String>>(listOf(GALLERY_ROOT)) }
    var favorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var photoSort by remember { mutableStateOf(SavedGalleryPhotoSort.NEWEST) }
    var folderFilter by remember { mutableStateOf(SavedGalleryFolderFilter.ALL) }
    var folderSort by remember { mutableStateOf(SavedGalleryFolderSort.NAME) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageLoadRevision by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("사진 확인 중") }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var initialLoadFinished by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<String?>(null) }
    var recentView by remember { mutableStateOf(false) }
    var defaultStorageView by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(true) }
    val galleryPreferences = remember { IosGalleryPreferences() }
    var gridColumns by remember { mutableStateOf(galleryPreferences.gridColumns()) }
    val gridState = key(folder, favoritesOnly, recentView, defaultStorageView) { rememberLazyGridState() }
    val listState = key(folder, favoritesOnly, recentView, defaultStorageView) { rememberLazyListState() }
    val headerScroll = key(folder, favoritesOnly, recentView, defaultStorageView) { rememberScrollState() }
    var pendingScrollAnchor by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showPhotoInfo by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf(false) }
    var viewerControls by remember { mutableStateOf(true) }
    LaunchedEffect(viewer) { if (viewer) viewerControls = true }
    var managing by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var batchSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deleteTarget by remember { mutableStateOf<Set<String>?>(null) }
    var moveTarget by remember { mutableStateOf<Set<String>?>(null) }
    var destination by remember { mutableStateOf<String?>(null) }
    var operationMessage by remember { mutableStateOf<String?>(null) }
    var sharing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName("dzlog.sharePhotoFinished", null, NSOperationQueue.mainQueue) { notification ->
            if (sharing) {
                sharing = false
                managing = false
                val outcome = notification?.`object` as? String
                operationMessage = when (outcome) {
                    "completed" -> "사진 공유를 완료했습니다."
                    "cancelled" -> "사진 공유를 취소했습니다."
                    else -> "사진 공유 실패: ${outcome ?: "결과를 확인할 수 없습니다."}"
                }
            }
        }
        onDispose { center.removeObserver(observer) }
    }
    var folderAction by remember { mutableStateOf<String?>(null) }
    var folderTarget by remember { mutableStateOf(GALLERY_ROOT) }
    var folderName by remember { mutableStateOf("") }
    var folderError by remember { mutableStateOf<String?>(null) }
    val thumbnails = remember { LinkedHashMap<String, ImageBitmap>() }
    val effectivePhotoSort = if (recentView) SavedGalleryPhotoSort.NEWEST else photoSort
    val scopedPhotos = when {
        recentView -> SavedGalleryEntryPolicy.recentAll(photos)
        defaultStorageView -> SavedGalleryEntryPolicy.defaultPhotos(photos)
        else -> photos
    }
    val folderPhotos = scopedPhotos.filter { folder == null || it.asset.target.relativePath == folder }
    val visible = effectivePhotoSort.sorted(folderPhotos.filter { entry ->
        !favoritesOnly || entry.asset.assetId in favorites
    })
    fun captureScrollAnchor(): Pair<String, Int>? {
        val index = if (grid) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
        val offset = if (grid) gridState.firstVisibleItemScrollOffset else listState.firstVisibleItemScrollOffset
        return visible.getOrNull(index)?.asset?.assetId?.let { it to offset }
    }
    fun changeColumns(next: Int) {
        val normalized = SavedGalleryGridPolicy.normalize(next)
        if (normalized == gridColumns || managing || refreshing) return
        pendingScrollAnchor = captureScrollAnchor()
        gridColumns = normalized
        galleryPreferences.setGridColumns(normalized)
    }
    LaunchedEffect(grid, gridColumns, pendingScrollAnchor) {
        val anchor = pendingScrollAnchor ?: return@LaunchedEffect
        val index = visible.indexOfFirst { it.asset.assetId == anchor.first }
        if (index >= 0) {
            if (grid) gridState.scrollToItem(index, anchor.second)
            else listState.scrollToItem(index, anchor.second)
        }
        pendingScrollAnchor = null
    }
    // A new scope starts at its first photo, rather than carrying an unrelated offset.
    LaunchedEffect(folder, favoritesOnly, recentView, defaultStorageView) {
        pendingScrollAnchor = null
    }
    val selectableIds = visible.filter { it.availability == CaptureMediaAvailability.PRESENT }
        .map { it.asset.assetId }.toSet()
    val selectedBatch = batchSelection.intersect(selectableIds)
    LaunchedEffect(selectableIds) { batchSelection = batchSelection.intersect(selectableIds) }
    fun selectEntry(entry: SavedCaptureMedia) {
        if (managing || refreshing) return
        val id = entry.asset.assetId
        if (selecting) {
            if (id in selectableIds) batchSelection = if (id in selectedBatch) selectedBatch - id else selectedBatch + id
        } else { selectedId = id; viewer = true }
    }
    fun enterSelection(entry: SavedCaptureMedia) {
        val next = SavedGallerySelectionPolicy.onLongPress(
            selectedBatch, entry.asset.assetId, selectableIds, managing || refreshing,
        ) ?: return
        batchSelection = next
        selecting = true
    }
    fun shareSelection(ids: Set<String>) {
        if (managing || sharing || refreshing) return
        val targets = effectivePhotoSort.sorted(photos.filter { it.asset.assetId in ids })
        if (targets.size != ids.size) { operationMessage = "선택한 사진을 다시 확인해 주세요."; return }
        managing = true
        scope.launch {
            try {
                SavedGalleryShareBudget.validateCount(targets.size)
                var totalBytes = 0L
                val files = targets.map { entry ->
                    val bytes = repository.readAsset(entry.asset.assetId) ?: error("선택한 사진을 읽을 수 없습니다.")
                    currentCoroutineContext().ensureActive()
                    totalBytes = SavedGalleryShareBudget.appendBytes(totalBytes, bytes.size)
                    bytes to entry.asset.target.displayName
                }
                operationMessage = "공유 화면 여는 중"
                sharing = true
                shareIosPhotos(files)
            } catch (cancelled: CancellationException) {
                sharing = false
                throw cancelled
            } catch (error: Throwable) {
                sharing = false
                operationMessage = "사진 공유 실패: ${error.message}"
            } finally { if (!sharing) managing = false }
        }
    }
    val selected = photos.firstOrNull { it.asset.assetId == selectedId }
    val destinations = buildSet {
        add(GALLERY_ROOT)
        addAll(folders)
        photos.forEach { entry ->
            var path = entry.asset.target.relativePath
            while (path.startsWith(GALLERY_ROOT) && path != GALLERY_ROOT) {
                add(path)
                path = path.trimEnd('/').substringBeforeLast('/') + "/"
            }
        }
    }.sorted()
    val currentFolder = folder
    val childFolders = currentFolder?.let {
        savedGalleryChildFolders(it, destinations, photos, folderFilter, folderSort)
    }.orEmpty()

    fun refresh() {
        if (refreshing || managing) return
        refreshing = true
        scope.launch {
            try {
                photos = repository.listSavedAssets().asReversed()
                folders = repository.listGalleryFolders()
                favorites = repository.favoriteGalleryAssetIds()
                imageLoadRevision += 1
                loadError = null
            } catch (error: Throwable) { loadError = "갤러리 조회 실패: ${error.message}" }
            finally { refreshing = false; initialLoadFinished = true }
        }
    }

    folderAction?.let { action ->
        AlertDialog(onDismissRequest = { if (!managing) folderAction = null },
            title = { Text(when (action) { "create" -> "폴더 생성"; "rename" -> "폴더 이름 변경"; else -> "빈 폴더 삭제" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (action == "delete") Text("이 폴더를 삭제합니다. 사진이나 하위 폴더가 있으면 먼저 이동하거나 정리해 주세요.")
                    else OutlinedTextField(value = folderName, onValueChange = { folderName = it; folderError = null },
                        enabled = !managing, singleLine = true, label = { Text("폴더 이름") })
                    folderError?.let { Text(it) }
                }
            },
            confirmButton = { TextButton(enabled = !managing && (action == "delete" || folderName.isNotBlank()), onClick = {
                managing = true
                scope.launch {
                    try {
                        folder = when (action) {
                            "create" -> repository.createGalleryFolder(folderTarget, folderName)
                            "rename" -> repository.renameGalleryFolder(folderTarget, folderName)
                            else -> {
                                repository.deleteEmptyGalleryFolder(folderTarget)
                                folderTarget.trimEnd('/').substringBeforeLast('/') + "/"
                            }
                        }
                        photos = repository.listSavedAssets().asReversed()
                        folders = repository.listGalleryFolders()
                        favorites = repository.favoriteGalleryAssetIds()
                        operationMessage = when (action) { "create" -> "폴더를 생성했습니다."; "rename" -> "폴더 이름을 변경했습니다."; else -> "빈 폴더를 삭제했습니다." }
                        folderAction = null
                    } catch (error: Throwable) { folderError = "폴더 처리 실패: ${error.message}" }
                    finally { managing = false }
                }
            }) { Text(if (action == "delete") "삭제" else "저장") } },
            dismissButton = { TextButton(enabled = !managing, onClick = { folderAction = null }) { Text("취소") } })
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(photos, folder, favoritesOnly, favorites, viewer, photoSort) {
        // A viewer can intentionally show the other capture companion outside
        // the filter. Reconcile the list preview when returning to the list.
        if (viewer) return@LaunchedEffect
        if (visible.none { it.asset.assetId == selectedId }) {
            val latestCapture = visible.firstOrNull()?.captureId
            selectedId = (visible.firstOrNull {
                it.captureId == latestCapture && it.asset.target.role == CaptureMediaRole.RESULT &&
                    it.availability == CaptureMediaAvailability.PRESENT
            } ?: visible.firstOrNull { it.availability == CaptureMediaAvailability.PRESENT }
                ?: visible.firstOrNull())?.asset?.assetId
        }
    }
    LaunchedEffect(selectedId, photos, folder, favoritesOnly, imageLoadRevision) {
        image = null
        val entry = photos.firstOrNull { it.asset.assetId == selectedId }
        if (entry == null) {
            message = when {
                photos.isEmpty() -> "저장된 사진이 없습니다."
                favoritesOnly && visible.isEmpty() -> "즐겨찾기 사진이 없습니다."
                else -> "폴더 안의 사진을 선택해 주세요."
            }
            return@LaunchedEffect
        }
        if (entry.availability != CaptureMediaAvailability.PRESENT) {
            message = if (entry.availability == CaptureMediaAvailability.INACCESSIBLE)
                "사진 접근 권한이 없습니다." else "저장된 사진을 찾을 수 없습니다."
            return@LaunchedEffect
        }
        message = "사진 불러오는 중"
        try {
            val bytes = repository.readAsset(entry.asset.assetId)
            // PhotoKit reads use a callback bridge that can finish after cancellation.
            // Do not let an older same-asset retry overwrite the newer load state.
            currentCoroutineContext().ensureActive()
            val restored = bytes?.takeIf { it.isNotEmpty() }?.let { SkiaImage.makeFromEncoded(uprightIosImage(it)).toComposeImageBitmap() }
            if (selectedId == entry.asset.assetId) {
                image = restored
                message = if (restored == null) "사진 파일을 읽을 수 없습니다." else "사진 복원 완료"
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) { if (selectedId == entry.asset.assetId) message = "사진 조회 실패: ${error.message}" }
    }

    if (showPhotoInfo) {
        val entry = selected
        val created = entry?.createdAtMillis?.let { millis ->
            NSDateFormatter().apply { dateFormat = "yyyy-MM-dd HH:mm:ss" }
                .stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
        } ?: "확인 불가"
        AlertDialog(onDismissRequest = { showPhotoInfo = false }, title = { Text("사진 정보") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("파일명: ${entry?.asset?.target?.displayName.orEmpty()}")
                    Text("폴더: ${entry?.asset?.target?.relativePath.orEmpty()}")
                    Text("사진 종류: ${if (entry?.asset?.target?.role == CaptureMediaRole.ORIGINAL) "원본" else "결과"}")
                    Text("사진 생성 시간: $created")
                    Text(image?.let { "표시 이미지 크기: ${it.width} × ${it.height}px" } ?: "표시 이미지 크기: 확인 불가")
                }
            }, confirmButton = { TextButton(onClick = { showPhotoInfo = false }) { Text("닫기") } })
    }

    deleteTarget?.let { targetIds ->
        AlertDialog(onDismissRequest = { deleteTarget = null },
            title = { Text("사진 삭제") },
            text = { Text("선택한 사진 ${targetIds.size}장을 iOS 사진 보관함에서 삭제합니다. 선택하지 않은 원본 또는 결과사진은 유지됩니다. iOS 확인 화면에서 취소할 수 있습니다.") },
            confirmButton = { TextButton(enabled = !managing, onClick = {
                if (managing) return@TextButton
                deleteTarget = null
                managing = true
                scope.launch {
                    try {
                        val removed = repository.deleteGalleryAssets(targetIds)
                        operationMessage = if (removed) "사진을 삭제했습니다." else "삭제가 취소되었거나 사진을 삭제할 수 없습니다."
                        if (removed) {
                            targetIds.forEach { thumbnails.remove(it) }
                            batchSelection = batchSelection - targetIds
                            selecting = false
                            image = null
                            viewer = false
                        }
                        photos = repository.listSavedAssets().asReversed()
                        folders = repository.listGalleryFolders()
                        favorites = repository.favoriteGalleryAssetIds()
                    } catch (error: Throwable) {
                        operationMessage = "삭제 결과 확인이 필요합니다: ${error.message}"
                    } finally { managing = false }
                }
            }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } })
    }
    moveTarget?.let { targetIds ->
        val targets = photos.filter { it.asset.assetId in targetIds }
        val sources = targets.map { it.asset.target.relativePath }.toSet()
        val duplicateNames = targets.map { it.asset.target.displayName }.distinct().size != targets.size
        AlertDialog(onDismissRequest = { moveTarget = null },
            title = { Text("사진 이동") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("선택한 사진 ${targetIds.size}장을 이동할 DZlog 폴더를 선택해 주세요. 선택하지 않은 사진과 iOS 사진 앱의 앨범은 변경되지 않습니다.")
                    if (duplicateNames) Text("같은 이름의 사진은 한 폴더로 함께 이동할 수 없습니다. 원본과 결과를 나눠 선택해 주세요.")
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        items(destinations) { path ->
                            Row(Modifier.fillMaxWidth().clickable { destination = path },
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = destination == path, onClick = { destination = path })
                                Text(path.removePrefix(GALLERY_ROOT).trimEnd('/').ifEmpty { "DZlog" })
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(enabled = !managing && !duplicateNames && destination != null && (sources.size != 1 || destination !in sources), onClick = {
                if (managing) return@TextButton
                val path = destination ?: return@TextButton
                moveTarget = null
                managing = true
                scope.launch {
                    try {
                        repository.moveGalleryAssets(targetIds, path)
                        photos = repository.listSavedAssets().asReversed()
                        folders = repository.listGalleryFolders()
                        recentView = false
                        defaultStorageView = false
                        folder = path
                        selectedId = targetIds.first()
                        batchSelection = emptySet()
                        selecting = false
                        operationMessage = "사진을 이동했습니다."
                    } catch (error: Throwable) { operationMessage = "사진 이동 실패: ${error.message}" }
                    finally { managing = false }
                }
            }) { Text("이동") } },
            dismissButton = { TextButton(onClick = { moveTarget = null }) { Text("취소") } })
    }

    Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (viewer) {
            // Every navigation entry point shares the active filtered order.
            val entries = visible
            val index = entries.indexOfFirst { it.asset.assetId == selectedId }
            fun navigate(delta: Int) {
                if (!managing && !refreshing && index >= 0 && index + delta in entries.indices) {
                    selectedId = entries[index + delta].asset.assetId
                }
            }
            if (viewerControls) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(enabled = !managing, onClick = { viewer = false }) { Text("사진 목록") }
                    TextButton(enabled = !managing && !refreshing, onClick = ::refresh) { Text("사진 다시 읽기") }
                    TextButton(enabled = !managing && selected != null, onClick = { showPhotoInfo = true }) { Text("사진 정보") }
                }
                loadError?.let { Text(it) }
                Text(selected?.asset?.target?.displayName.orEmpty())
                Text(message)
                operationMessage?.let { Text(it) }
                if (managing) Text("사진 처리 중")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !managing && !refreshing, onClick = { viewerControls = false }) { Text("조작부 숨김") }
                    TextButton(enabled = !managing && selected != null, onClick = {
                        val id = selectedId ?: return@TextButton
                        managing = true
                        scope.launch {
                            try {
                                val favorite = id !in favorites
                                repository.setGalleryFavorite(id, favorite)
                                favorites = repository.favoriteGalleryAssetIds()
                                operationMessage = if (favorite) "즐겨찾기에 추가했습니다." else "즐겨찾기에서 해제했습니다."
                            } catch (error: Throwable) { operationMessage = "즐겨찾기 변경 실패: ${error.message}" }
                            finally { managing = false }
                        }
                    }) { Text(if (selectedId in favorites) "★ 즐겨찾기" else "☆ 즐겨찾기") }
                    TextButton(enabled = !managing && !refreshing && selected?.availability == CaptureMediaAvailability.PRESENT,
                        onClick = { selectedId?.let { shareSelection(setOf(it)) } }) { Text("사진 공유") }
                    TextButton(enabled = !managing && !refreshing && selected?.availability == CaptureMediaAvailability.PRESENT,
                        onClick = { moveTarget = selectedId?.let { setOf(it) }; destination = selected?.asset?.target?.relativePath }) { Text("사진 이동") }
                    TextButton(enabled = !managing && !refreshing && selected?.availability == CaptureMediaAvailability.PRESENT,
                        onClick = { deleteTarget = selectedId?.let { setOf(it) } }) { Text("사진 삭제") }
                }
            } else {
                // Always reachable even when the image is unavailable or gestures are inaccessible.
                TextButton(onClick = { viewerControls = true }) { Text("조작부 표시") }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                key(selectedId, image) {
                    image?.let { IosGalleryImageViewer(it, enabled = !managing && !refreshing,
                        showControls = viewerControls,
                        onToggleControls = { viewerControls = !viewerControls },
                        onPrevious = { navigate(-1) }, onNext = { navigate(1) },
                        onShowInfo = { if (selected != null) showPhotoInfo = true }) }
                }
            }
            // A companion may live outside the active folder/favorites filter.
            // Do not silently expand previous/next navigation to the entire library.
            if (viewerControls) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), enabled = !managing && !refreshing && index > 0, onClick = { navigate(-1) }) { Text("이전 사진") }
                val companion = photos.firstOrNull { it.captureId == selected?.captureId &&
                    it.asset.target.role != selected?.asset?.target?.role && it.availability == CaptureMediaAvailability.PRESENT }
                if (companion != null) Button(modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), enabled = !managing && !refreshing, onClick = {
                    if (!managing && !refreshing) selectedId = companion.asset.assetId
                }) {
                    Text(if (companion.asset.target.role == CaptureMediaRole.ORIGINAL) "원본 보기" else "결과 보기")
                }
                Button(modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), enabled = !managing && !refreshing && index >= 0 && index < entries.lastIndex,
                    onClick = { navigate(1) }) { Text("다음 사진") }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val controlsMaxHeight = (maxHeight * 0.75f - 8.dp).coerceAtLeast(0.dp)
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Leave at least a quarter of the available height for photos.
                    // The controls scroll independently instead of starving the grid.
                    Column(Modifier.fillMaxWidth().heightIn(max = controlsMaxHeight)
                        .verticalScroll(headerScroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("갤러리", style = MaterialTheme.typography.titleLarge)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = {
                                when {
                                    selecting -> { selecting = false; batchSelection = emptySet() }
                                    recentView || defaultStorageView -> { recentView = false; defaultStorageView = false; folder = null }
                                    else -> onBack()
                                }
                            }, enabled = !managing) { Text("뒤로가기") }
                            Button(onClick = ::refresh, enabled = !refreshing && !managing) { Text("새로고침") }
                            if (grid && !selecting) GalleryChoiceMenu("그리드 ${gridColumns}열",
                                (SavedGalleryGridPolicy.MIN_COLUMNS..SavedGalleryGridPolicy.MAX_COLUMNS).map { "${it}열" },
                                enabled = !managing && !refreshing) { changeColumns(it + SavedGalleryGridPolicy.MIN_COLUMNS) }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(enabled = !managing && !refreshing, onClick = {
                                recentView = false; defaultStorageView = false
                                folder = if (folder == null) GALLERY_ROOT else null
                            }) {
                                Text(if (folder == null) "폴더 보기" else "전체 사진")
                            }
                            TextButton(enabled = !managing && !refreshing, onClick = { pendingScrollAnchor = captureScrollAnchor(); grid = !grid }) { Text(if (grid) "목록 보기" else "그리드 보기") }
                            TextButton(enabled = !managing && !refreshing, onClick = { favoritesOnly = !favoritesOnly }) {
                                Text(if (favoritesOnly) "전체 보기" else "즐겨찾기")
                            }
                            if (folder != null && folder != GALLERY_ROOT) TextButton(enabled = !managing && !refreshing, onClick = {
                                folder = folder!!.trimEnd('/').substringBeforeLast('/') + "/"
                            }) { Text("상위 폴더") }
                        }
                        if (folder == null && !recentView && !favoritesOnly && !selecting) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !managing && !refreshing, onClick = {
                                    recentView = true; defaultStorageView = false; batchSelection = emptySet()
                                }) { Text("최근 촬영") }
                                TextButton(enabled = !managing && !refreshing, onClick = {
                                    defaultStorageView = true; recentView = false; folder = GALLERY_ROOT; batchSelection = emptySet()
                                }) { Text("기본 저장") }
                            }
                        }
                        if (recentView) Text("최근 촬영 · 전체 사진 최신순")
                        if (defaultStorageView) {
                            Text("기본 저장 · DZlog 바로 아래 사진")
                            val originalCount = SavedGalleryEntryPolicy.rootOriginalPhotos(photos).size
                            if (originalCount > 0 && !selecting) TextButton(enabled = !managing && !refreshing, onClick = {
                                defaultStorageView = false; folder = GALLERY_ROOT + "original/"
                            }) { Text("기본 저장 원본 ${originalCount}개") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("사진 ${visible.size}개")
                            if (!selecting && !recentView) GalleryChoiceMenu("사진 정렬 · ${photoSort.label}", SavedGalleryPhotoSort.entries.map { it.label },
                                enabled = !managing && !refreshing) { photoSort = SavedGalleryPhotoSort.entries[it] }
                            TextButton(enabled = !managing && !refreshing, onClick = {
                                selecting = !selecting; batchSelection = emptySet()
                            }) { Text(if (selecting) "선택 취소" else "선택") }
                        }
                        if (selecting) {
                            Text("선택 ${selectedBatch.size}개")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !managing && !refreshing && selectableIds.isNotEmpty(),
                                    onClick = { batchSelection = selectableIds }) { Text("전체 선택") }
                                TextButton(enabled = !managing && selectedBatch.isNotEmpty(),
                                    onClick = { batchSelection = emptySet() }) { Text("선택 해제") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !managing && !refreshing && selectedBatch.isNotEmpty(), onClick = {
                                    moveTarget = selectedBatch.toSet(); destination = null
                                }) { Text("선택 이동") }
                                TextButton(enabled = !managing && !refreshing && selectedBatch.isNotEmpty(), onClick = {
                                    deleteTarget = selectedBatch.toSet()
                                }) { Text("선택 삭제") }
                                TextButton(enabled = !managing && !refreshing && selectedBatch.isNotEmpty(),
                                    onClick = { shareSelection(selectedBatch.toSet()) }) { Text("선택 공유") }
                            }
                        }
                        currentFolder?.let { Text("폴더: $it") }
                        currentFolder?.takeUnless { selecting || defaultStorageView }?.let { path ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GalleryChoiceMenu("폴더 · ${folderFilter.label}", SavedGalleryFolderFilter.entries.map { it.label },
                                    enabled = !managing && !refreshing) { folderFilter = SavedGalleryFolderFilter.entries[it] }
                                GalleryChoiceMenu("폴더순 · ${folderSort.label}", SavedGalleryFolderSort.entries.map { it.label },
                                    enabled = !managing && !refreshing) { folderSort = SavedGalleryFolderSort.entries[it] }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(enabled = !managing && !refreshing, onClick = {
                                    folderTarget = path; folderName = ""; folderError = null; folderAction = "create"
                                }) { Text("폴더 생성") }
                                if (path != GALLERY_ROOT) {
                                    TextButton(enabled = !managing && !refreshing, onClick = {
                                        folderTarget = path; folderName = path.trimEnd('/').substringAfterLast('/'); folderError = null; folderAction = "rename"
                                    }) { Text("이름 변경") }
                                    TextButton(enabled = !managing && !refreshing, onClick = {
                                        folderTarget = path; folderError = null; folderAction = "delete"
                                    }) { Text("폴더 삭제") }
                                }
                            }
                        }
                        loadError?.let { Text(it) }
                        operationMessage?.let { Text(it) }
                        if (!selecting && !defaultStorageView && childFolders.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().heightIn(max = 100.dp)) {
                            items(childFolders, key = { it.path }) { summary ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(enabled = !managing, onClick = { folder = summary.path }) {
                                        Text(summary.path.removePrefix(currentFolder.orEmpty()).trimEnd('/'))
                                    }
                                    Text("사진 기록 ${summary.photoCount}개 · 하위 포함")
                                }
                            }
                        }
                        if (!defaultStorageView && currentFolder != null && childFolders.isEmpty() && folderFilter != SavedGalleryFolderFilter.ALL) {
                            Text("조건에 맞는 하위 폴더가 없습니다.")
                        }
                        Text(message)
                        if (!selecting && selected != null && visible.any { it.asset.assetId == selectedId }) image?.let {
                            Image(it, "갤러리 사진 미리보기", Modifier.fillMaxWidth().height(100.dp).clickable { viewer = true },
                                contentScale = ContentScale.Fit)
                        }
                    }
                    val emptyState = SavedGalleryEmptyStatePolicy.resolve(
                        loading = refreshing || !initialLoadFinished,
                        failed = loadError != null,
                        scopeCount = folderPhotos.size,
                        visibleCount = visible.size,
                        favoritesOnly = favoritesOnly,
                    )
                    if (emptyState != null) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text(when (emptyState) {
                                SavedGalleryEmptyState.NO_PHOTOS -> "사진이 없습니다."
                                SavedGalleryEmptyState.NO_FAVORITES -> "즐겨찾기한 사진이 없습니다."
                            })
                        }
                    } else if (grid) LazyVerticalGrid(GridCells.Fixed(gridColumns), state = gridState,
                        modifier = Modifier.weight(1f).iosGalleryGridPinch(gridColumns, !managing && !refreshing && !selecting, ::changeColumns),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gridItems(visible, key = { it.asset.assetId }) { entry ->
                            val thumbnail by produceState<ImageBitmap?>(
                                if (entry.availability == CaptureMediaAvailability.PRESENT) thumbnails[entry.asset.assetId] else null,
                                entry.asset.assetId, entry.availability) {
                                if (entry.availability != CaptureMediaAvailability.PRESENT) {
                                    value = null
                                    thumbnails.remove(entry.asset.assetId)
                                } else if (value == null) {
                                    value = runCatching { repository.readThumbnail(entry.asset.assetId)?.let {
                                        SkiaImage.makeFromEncoded(uprightIosImage(it)).toComposeImageBitmap()
                                    } }.getOrNull()
                                    value?.let {
                                        if (thumbnails.size >= 32) thumbnails.remove(thumbnails.keys.first())
                                        thumbnails[entry.asset.assetId] = it
                                    }
                                }
                            }
                            Column(Modifier.fillMaxWidth().combinedClickable(
                                enabled = !managing && !refreshing,
                                onClick = { selectEntry(entry) },
                                onLongClick = { enterSelection(entry) },
                            )) {
                                if (selecting) Checkbox(modifier = Modifier.semantics {
                                    contentDescription = "사진 선택 · ${if (entry.asset.target.role == CaptureMediaRole.ORIGINAL) "원본" else "결과"} · ${entry.asset.target.displayName}"
                                }, checked = entry.asset.assetId in selectedBatch,
                                    enabled = !managing && !refreshing && entry.asset.assetId in selectableIds,
                                    onCheckedChange = { selectEntry(entry) })
                                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(DDZColor.Surface), contentAlignment = Alignment.Center) {
                                    if (thumbnail != null) Image(thumbnail!!, "사진 썸네일", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Text(if (entry.availability == CaptureMediaAvailability.PRESENT) "사진" else "접근 불가")
                                }
                                Text((if (entry.asset.assetId in favorites) "★ " else "") +
                                    if (entry.asset.target.role == CaptureMediaRole.RESULT) "결과" else "원본")
                                Text(entry.asset.target.displayName, maxLines = 1)
                            }
                        }
                    } else LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visible, key = { it.asset.assetId }) { entry ->
                            Column(Modifier.fillMaxWidth().combinedClickable(
                                enabled = !managing && !refreshing,
                                onClick = { selectEntry(entry) },
                                onLongClick = { enterSelection(entry) },
                            ).padding(8.dp)) {
                                if (selecting) Checkbox(modifier = Modifier.semantics {
                                    contentDescription = "사진 선택 · ${if (entry.asset.target.role == CaptureMediaRole.ORIGINAL) "원본" else "결과"} · ${entry.asset.target.displayName}"
                                }, checked = entry.asset.assetId in selectedBatch,
                                    enabled = !managing && !refreshing && entry.asset.assetId in selectableIds,
                                    onCheckedChange = { selectEntry(entry) })
                                Text((if (entry.asset.assetId in favorites) "★ " else "") +
                                    "${entry.asset.target.displayName} · " + if (entry.asset.target.role == CaptureMediaRole.RESULT) "결과" else "원본")
                                Text(entry.asset.target.relativePath)
                                Text(when (entry.availability) {
                                    CaptureMediaAvailability.PRESENT -> "사진 보관됨"
                                    CaptureMediaAvailability.INACCESSIBLE -> "사진 접근 불가"
                                    CaptureMediaAvailability.MISSING -> "사진 누락"
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryChoiceMenu(label: String, options: List<String>, enabled: Boolean, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(enabled = enabled, onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, enabled = enabled, onClick = {
                    onSelected(index)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun shareIosPhotos(files: List<Pair<ByteArray, String>>) {
    val payload = files.map { (bytes, filename) ->
        require(bytes.isNotEmpty())
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        mapOf("data" to data, "filename" to filename)
    }
    NSNotificationCenter.defaultCenter.postNotificationName("dzlog.sharePhoto", payload)
}
