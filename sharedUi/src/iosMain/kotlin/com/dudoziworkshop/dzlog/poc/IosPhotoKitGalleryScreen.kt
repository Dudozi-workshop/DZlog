package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderOperationPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.CapturePathImpact
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeleteScope
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderNamePolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderMovePolicy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.*
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import org.jetbrains.skia.Image as SkiaImage
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.create

private const val GALLERY_ROOT = GalleryFolderIndexPolicy.ROOT

/** Browses the app's indexed Photos assets; logical folders never rename Photos resources. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IosPhotoKitGalleryScreen(
    repository: PhotoKitCaptureMediaRepository,
    onBack: () -> Unit,
    resultCaptureIds: List<String>? = null,
    initialAssetId: String? = null,
    initialFolder: String? = null,
    capturePathImpact: ((String) -> CapturePathImpact)? = null,
) {
    val permissions = rememberIosPermissions()
    val photoScopeVersion = permissions.photoScopeVersion
    if (!permissions.photos.readable) {
        Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.dudoziworkshop.dzlog.ui.common.DDZTopBar(title = "갤러리", onBack = onBack)
            PermissionCard(camera = false, access = permissions.photos)
        }
        return
    }
    val scope = rememberCoroutineScope()
    var photos by remember { mutableStateOf<List<SavedCaptureMedia>>(emptyList()) }
    var folders by remember { mutableStateOf<List<String>>(listOf(GALLERY_ROOT)) }
    var favorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var photoSort by remember { mutableStateOf(SavedGalleryPhotoSort.NEWEST) }
    var folderFilter by remember { mutableStateOf(SavedGalleryFolderFilter.ALL) }
    var folderSort by remember { mutableStateOf(SavedGalleryFolderSort.NAME) }
    var selectedId by remember { mutableStateOf(initialAssetId) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageLoadRevision by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("사진 확인 중") }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var initialLoadFinished by remember { mutableStateOf(false) }
    var folder by remember(initialFolder) { mutableStateOf(initialFolder) }
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
    var viewer by remember { mutableStateOf(initialAssetId != null) }
    var movedInViewer by remember { mutableStateOf(emptySet<String>()) }
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
    var folderDestination by remember { mutableStateOf<String?>(null) }
    var folderTarget by remember { mutableStateOf(GALLERY_ROOT) }
    var folderName by remember { mutableStateOf("") }
    var folderError by remember { mutableStateOf<String?>(null) }
    fun openFolderAction(path: String, action: String) {
        folderTarget = path
        folderDestination = null
        folderName = if (action == "rename") path.trimEnd('/').substringAfterLast('/') else ""
        folderError = null
        folderAction = action
    }
    val thumbnails = remember(photoScopeVersion) { LinkedHashMap<String, ImageBitmap>() }
    val effectivePhotoSort = if (recentView) SavedGalleryPhotoSort.NEWEST else photoSort
    val scopedPhotos = when {
        recentView -> SavedGalleryEntryPolicy.recentAll(photos)
        defaultStorageView -> SavedGalleryEntryPolicy.defaultPhotos(photos)
        else -> photos
    }
    val currentFolder = folder
    val destinations = remember(photos, folders) {
        buildSet {
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
    }
    val folderListing = remember(currentFolder, destinations, photos, folderFilter, folderSort) {
        currentFolder?.let { savedGalleryFolderListing(it, destinations, photos, folderFilter, folderSort) }
    }
    val childFolders = folderListing?.childFolders.orEmpty()
    val photoFolder = currentFolder?.let { if (folderListing?.showsOriginalsDirectly == true) it + "original/" else it }
    val folderPhotos = scopedPhotos.filter { photoFolder == null || it.asset.target.relativePath == photoFolder }
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
    fun openViewer(id: String) {
        movedInViewer = emptySet()
        selectedId = id
        viewer = true
    }
    fun selectEntry(entry: SavedCaptureMedia) {
        if (managing || refreshing) return
        val id = entry.asset.assetId
        if (selecting) {
            if (id in selectableIds) batchSelection = if (id in selectedBatch) selectedBatch - id else selectedBatch + id
        } else openViewer(id)
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
    val scopeIds = remember(resultCaptureIds) { resultCaptureIds?.toSet() }
    suspend fun readScopedPhotos(): List<SavedCaptureMedia> = repository.listSavedAssets()
        .filter { scopeIds == null || it.captureId in scopeIds }
        .asReversed()

    fun viewerPhotos(entries: List<SavedCaptureMedia>) =
        if (movedInViewer.isEmpty()) entries else entries.filterNot { it.asset.assetId in movedInViewer }

    val resultPages = remember(resultCaptureIds, photos, movedInViewer) {
        resultCaptureIds?.let { ids -> quickResultCaptures(ids.asReversed(), viewerPhotos(photos)) }
    }
    val resultEntries = remember(resultPages, photos, selectedId) {
        resultPages?.let { pages ->
            val byId = photos.associateBy { it.asset.assetId }
            pages.mapNotNull { page ->
                val id = if (selectedId != null && selectedId == page.original?.uri) selectedId else page.displayedPhoto.uri
                byId[id]
            }
        }
    }

    val viewerEntries = resultEntries ?: viewerPhotos(visible)

    fun selectAfterViewerRemoval(removedIds: Set<String>): String? {
        val entries = viewerEntries
        val currentIndex = entries.indexOfFirst { it.asset.assetId == selectedId }
        // A companion opened from another folder still belongs to the same capture.
        val anchor = if (currentIndex >= 0) currentIndex else entries.indexOfFirst { it.captureId == selected?.captureId }
        val pages = resultPages?.map { page ->
            listOfNotNull(page.result?.uri, page.original?.uri)
        } ?: entries.map { entry ->
            if (entry.availability == CaptureMediaAvailability.PRESENT) listOf(entry.asset.assetId) else emptyList()
        }
        return savedGalleryPhotoAfterRemoval(pages, anchor, removedIds)
    }

    fun refresh() {
        if (refreshing || managing) return
        refreshing = true
        scope.launch {
            try {
                val previousCapture = selected?.captureId
                photos = readScopedPhotos()
                if (resultCaptureIds != null && photos.none {
                    it.asset.assetId == selectedId && it.availability == CaptureMediaAvailability.PRESENT
                }) {
                    val pages = quickResultCaptures(resultCaptureIds.asReversed(), viewerPhotos(photos))
                    val ids = resultCaptureIds.asReversed()
                    selectedId = (pages.firstOrNull { ids[it.captureIndex] == previousCapture }
                        ?: pages.firstOrNull())?.displayedPhoto?.uri
                }
                folders = repository.listGalleryFolders()
                favorites = repository.favoriteGalleryAssetIds()
                imageLoadRevision += 1
                loadError = null
            } catch (error: Throwable) { loadError = "갤러리 조회 실패: ${error.message}" }
            finally { refreshing = false; initialLoadFinished = true }
        }
    }

    folderAction?.let { action ->
        var deletion by remember(action, folderTarget) { mutableStateOf<PhotoKitFolderDeletion?>(null) }
        var inspection by remember(action, folderTarget) { mutableIntStateOf(0) }
        var pickingFolder by remember(action, folderTarget) { mutableStateOf(action == "move") }
        var browsePath by remember(action, folderTarget) { mutableStateOf(GALLERY_ROOT) }
        if (action == "move" && pickingFolder) {
            val catalog = rememberMoveCatalog(repository, photoScopeVersion, inspection)
            val snapshot = catalog?.getOrNull()
            IosGalleryMoveDestinationPicker(
                path = browsePath, photos = snapshot?.first.orEmpty(), folders = snapshot?.second.orEmpty(),
                movingFolder = folderTarget, loading = catalog == null,
                error = catalog?.exceptionOrNull()?.let { it.message ?: "이동할 폴더를 불러오지 못했습니다." },
                onNavigate = { browsePath = it }, onRetry = { inspection++ },
                onCancel = { if (folderDestination == null) folderAction = null else pickingFolder = false },
                onChoose = { folderDestination = browsePath; folderError = null; pickingFolder = false },
                thumbnail = { entry -> galleryThumbnail(entry, repository, thumbnails, photoScopeVersion) },
            )
            return@let
        }
        if (action == "delete") {
            LaunchedEffect(folderTarget, inspection) {
                try {
                    deletion = repository.prepareFolderDeletion(folderTarget)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    folderError = error.message ?: "삭제 범위를 확인하지 못했습니다."
                }
            }
        }
        val renamePlan = if (action == "rename") remember(folderTarget, folderName, folders) {
            runCatching { GalleryFolderOperationPolicy.checkedRenamedPath(folderTarget, folderName, folders) }
        } else null
        AlertDialog(onDismissRequest = { if (!managing) folderAction = null },
            title = { Text(when (action) { "create" -> "폴더 생성"; "rename" -> "폴더 이름 변경"; "move" -> "폴더 이동"; else -> "이 폴더를 삭제할까요?" }) },
            text = {
                Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (action == "delete") {
                        Text(folderTarget)
                        val plan = deletion
                        if (plan != null) {
                            Text("결과 사진 ${plan.resultCount}장 · 원본 사진 ${plan.originalCount}장")
                            Text("하위 폴더 ${plan.folderCount}개")
                            if (plan.missingCount > 0) Text("사진 보관함에서 이미 삭제된 ${plan.missingCount}장의 앱 기록도 정리합니다.")
                            if (plan.resultCount + plan.originalCount > 0) Text("포함된 사진은 iPhone 사진 보관함에서도 삭제됩니다.")
                        } else if (folderError == null) Text("삭제 범위를 확인하고 있습니다.")
                        else {
                            TextButton(enabled = !managing, onClick = {
                                folderError = null
                                inspection++
                            }) { Text("다시 확인") }
                            if (permissions.photos == IosAccess.LIMITED) {
                                TextButton(onClick = ::openIosSystemSettings) { Text("iPhone 설정 열기") }
                            }
                        }
                    }
                    else if (action == "move") {
                        val destination = checkNotNull(folderDestination)
                        Text("현재 경로")
                        Text(folderTarget)
                        Text("이동 후 경로")
                        Text(GalleryFolderMovePolicy.movedPath(folderTarget, destination))
                        TextButton(enabled = !managing, onClick = {
                            browsePath = destination
                            pickingFolder = true
                        }) { Text("이동 위치 변경") }
                    }
                    else {
                        val rename = action == "rename"
                        if (rename) {
                            Text("현재 경로")
                            Text(folderTarget)
                        }
                        val label = if (rename) "새 폴더 이름" else "폴더 이름"
                        OutlinedTextField(value = folderName, onValueChange = { folderName = it; folderError = null },
                            modifier = Modifier.semantics { contentDescription = label },
                            enabled = !managing, singleLine = true, label = { Text(label) })
                        if (rename) {
                            Text("변경 후 경로")
                            Text(renamePlan?.getOrNull() ?: "새 이름을 확인해 주세요.")
                            renamePlan?.exceptionOrNull()?.message?.let { Text(it, color = DDZColor.Destructive) }
                        }
                    }
                    if (action == "rename" || action == "move") {
                        Text(when (capturePathImpact?.invoke(folderTarget)) {
                            CapturePathImpact.MATCH -> "촬영 저장설정에 이 폴더 경로가 있습니다. 변경 후 촬영 저장경로를 확인해 주세요."
                            CapturePathImpact.POSSIBLE -> "날짜·표 값에 따라 이 폴더가 촬영 경로가 될 수 있습니다. 변경 후 촬영 저장경로를 확인해 주세요."
                            else -> if (action == "move") "사진·원본·하위 폴더도 함께 이동합니다. 다음 촬영의 저장 경로는 현재 설정을 따릅니다."
                                else "촬영 저장설정은 변경되지 않습니다."
                        }, style = DDZTypography.Caption)
                    }
                    folderError?.let { Text(it) }
                }
            },
            confirmButton = { TextButton(enabled = !managing && when (action) {
                "rename" -> renamePlan?.isSuccess == true
                "move" -> folderDestination != null
                "delete" -> deletion != null
                else -> folderName.isNotBlank()
            }, onClick = {
                managing = true
                scope.launch {
                    try {
                        val updatedFolder = when (action) {
                            "create" -> repository.createGalleryFolder(folderTarget, folderName)
                            "move" -> repository.moveGalleryFolder(folderTarget, checkNotNull(folderDestination))
                            "rename" -> repository.renameGalleryFolder(folderTarget, folderName)
                            else -> {
                                repository.deleteGalleryFolder(checkNotNull(deletion))
                                folderTarget.trimEnd('/').substringBeforeLast('/') + "/"
                            }
                        }
                        if (action == "create" || folder == folderTarget) folder = updatedFolder
                        operationMessage = when (action) { "create" -> "폴더를 생성했습니다."; "rename" -> "폴더 이름을 변경했습니다."; "move" -> "폴더를 이동했습니다."; else -> "폴더를 삭제했습니다." }
                        folderAction = null
                        try {
                            photos = readScopedPhotos()
                            folders = repository.listGalleryFolders()
                            favorites = repository.favoriteGalleryAssetIds()
                            loadError = null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            loadError = "폴더 변경 후 갤러리 조회 실패: ${error.message}"
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        if (action == "delete") deletion = null
                        folderError = error.message ?: "폴더를 처리하지 못했습니다."
                    }
                    finally { managing = false }
                }
            }) { Text(when (action) { "delete" -> "삭제"; "move" -> "이동"; "rename" -> "변경 확정"; else -> "저장" }) } },
            dismissButton = { TextButton(enabled = !managing, onClick = { folderAction = null }) { Text("취소") } })
    }
    var refreshedScope by remember { mutableIntStateOf(-1) }
    LaunchedEffect(photoScopeVersion, managing, refreshing) {
        if (!managing && !refreshing && refreshedScope != photoScopeVersion) {
            refreshedScope = photoScopeVersion
            refresh()
        }
    }
    LaunchedEffect(visible, viewer) {
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
    LaunchedEffect(selectedId, photos, folder, favoritesOnly, imageLoadRevision, photoScopeVersion) {
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
                            if (viewer) {
                                selectedId = selectAfterViewerRemoval(targetIds)
                                if (selectedId == null) {
                                    if (initialAssetId != null) {
                                        onBack()
                                        return@launch
                                    }
                                    viewer = false
                                }
                            }
                            photos = photos.filterNot { it.asset.assetId in targetIds }
                        }
                        photos = readScopedPhotos()
                        folders = repository.listGalleryFolders()
                        favorites = repository.favoriteGalleryAssetIds()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        operationMessage = "삭제 결과 확인이 필요합니다: ${error.message}"
                    } finally { managing = false }
                }
            }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } })
    }
    moveTarget?.let { targetIds ->
        var browsePath by remember(targetIds) { mutableStateOf(GALLERY_ROOT) }
        var retry by remember(targetIds) { mutableIntStateOf(0) }
        // Quick results only contain the current captures. A move needs the entire gallery.
        val catalog = rememberMoveCatalog(repository, photoScopeVersion, retry)
        val snapshot = catalog?.getOrNull()
        if (destination == null || snapshot == null) {
            IosGalleryMoveDestinationPicker(
                path = browsePath, photos = snapshot?.first.orEmpty(), folders = snapshot?.second.orEmpty(),
                selectedIds = targetIds, loading = catalog == null,
                error = catalog?.exceptionOrNull()?.let { it.message ?: "이동할 폴더를 불러오지 못했습니다." },
                onNavigate = { browsePath = it }, onRetry = { retry++ }, onCancel = { moveTarget = null },
                onChoose = { destination = browsePath },
                thumbnail = { entry -> galleryThumbnail(entry, repository, thumbnails, photoScopeVersion) },
            )
            return@let
        }
        val libraryPhotos = snapshot.first
        val originals = remember(libraryPhotos, targetIds) { SavedGalleryMovePolicy.additionalOriginals(libraryPhotos, targetIds) }
        var includeOriginals by remember(targetIds) { mutableStateOf<Boolean?>(null) }
        val plan = remember(libraryPhotos, targetIds, destination, includeOriginals) {
            destination?.let { path -> runCatching {
                SavedGalleryMovePolicy.destinations(libraryPhotos, targetIds, path, includeOriginals == true)
            } }
        }
        val paths = plan?.getOrNull()
        val movingPhotos = remember(libraryPhotos, paths) { libraryPhotos.filter { paths?.containsKey(it.asset.assetId) == true } }
        val sources = movingPhotos.associate { it.asset.assetId to it.asset.target.relativePath }
        var reviewing by remember(targetIds, paths, sources, includeOriginals) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { moveTarget = null },
            title = { Text(if (reviewing) "사진 이동 확인" else "사진 이동") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (reviewing && paths != null) {
                        val sourceFolders = sources.values.distinct()
                        Text("현재 경로")
                        Text(sourceFolders.singleOrNull() ?: "${sourceFolders.size}개 폴더에서 선택")
                        Text("이동 후 경로")
                        val byRole = movingPhotos.groupBy { it.asset.target.role }
                        for ((role, label) in listOf(CaptureMediaRole.RESULT to "결과사진", CaptureMediaRole.ORIGINAL to "원본사진")) {
                            val items = byRole[role] ?: continue
                            Text("$label ${items.size}장 · ${paths.getValue(items.first().asset.assetId)}")
                        }
                        Text("촬영 저장설정은 변경되지 않습니다.")
                        TextButton(onClick = { reviewing = false }) { Text("이동 선택 변경") }
                    } else {
                        Text("이동할 폴더: ${destination.orEmpty()}")
                        TextButton(onClick = { destination = null }) { Text("이동 위치 변경") }
                        if (originals.isNotEmpty()) {
                            Text("연결된 원본 ${originals.size}장")
                            Row {
                                TextButton(onClick = { includeOriginals = false }) { Text(if (includeOriginals == false) "✓ 사진만" else "사진만") }
                                TextButton(onClick = { includeOriginals = true }) { Text(if (includeOriginals == true) "✓ 원본 포함" else "원본 포함") }
                            }
                        }
                        plan?.exceptionOrNull()?.message?.let { Text(it) }
                    }
                }
            },
            confirmButton = { TextButton(enabled = !managing && paths != null && (originals.isEmpty() || includeOriginals != null), onClick = {
                if (managing || paths == null) return@TextButton
                if (!reviewing) {
                    reviewing = true
                    return@TextButton
                }
                val path = destination ?: return@TextButton
                val withOriginals = includeOriginals == true
                moveTarget = null
                managing = true
                scope.launch {
                    try {
                        val moved = repository.moveGalleryAssets(targetIds, path, withOriginals, paths, sources)
                        if (viewer) {
                            selectedId = selectAfterViewerRemoval(moved.keys)
                            movedInViewer = movedInViewer + moved.keys
                            if (selectedId == null) {
                                if (initialAssetId != null) {
                                    onBack()
                                    return@launch
                                }
                                viewer = false
                            }
                        } else {
                            recentView = false
                            defaultStorageView = false
                            folder = moved.values.distinct().singleOrNull() ?: path
                            selectedId = targetIds.first()
                        }
                        batchSelection = emptySet()
                        selecting = false
                        operationMessage = "사진을 이동했습니다."
                        // The move is already committed even if a subsequent Photos read fails.
                        photos = photos.map { entry ->
                            val newPath = moved[entry.asset.assetId] ?: return@map entry
                            entry.copy(
                                asset = entry.asset.copy(target = entry.asset.target.copy(relativePath = newPath)),
                            )
                        }
                        try {
                            photos = readScopedPhotos()
                            folders = repository.listGalleryFolders()
                            loadError = null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            loadError = "이동 후 갤러리 조회 실패: ${error.message}"
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) { operationMessage = "사진 이동 실패: ${error.message}" }
                    finally { managing = false }
                }
            }) { Text(if (reviewing) "이동" else "다음") } },
            dismissButton = { TextButton(onClick = { moveTarget = null }) { Text("취소") } })
    }

    Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (permissions.photos == IosAccess.LIMITED && (!viewer || viewerControls)) {
            TextButton(enabled = !managing && !refreshing, onClick = {
                NSNotificationCenter.defaultCenter.postNotificationName("dzlog.permissionAction", "selection")
            }) { Text("선택한 사진 변경") }
        }
        if (viewer) {
            // Every navigation entry point shares the active filtered order.
            val entries = viewerEntries
            val index = entries.indexOfFirst { it.asset.assetId == selectedId }
            fun navigate(delta: Int) {
                if (!managing && !refreshing && index >= 0 && index + delta in entries.indices) {
                    selectedId = entries[index + delta].asset.assetId
                }
            }
            if (viewerControls) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(enabled = !managing, onClick = {
                        if (initialAssetId != null) onBack() else viewer = false
                    }) { Text(if (initialAssetId != null) "촬영 결과" else "사진 목록") }
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
                        onClick = { moveTarget = selectedId?.let { setOf(it) }; destination = null }) { Text("사진 이동") }
                    TextButton(enabled = !managing && !refreshing && selected?.availability == CaptureMediaAvailability.PRESENT,
                        onClick = { deleteTarget = selectedId?.let { setOf(it) } }) { Text("사진 삭제") }
                }
            } else {
                // Always reachable even when the image is unavailable or gestures are inaccessible.
                TextButton(onClick = { viewerControls = true }) { Text("조작부 표시") }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (index >= 0 && entries.isNotEmpty()) IosGalleryAnimatedPager(
                    repository = repository,
                    entries = entries,
                    selectedId = selectedId,
                    imageLoadRevision = imageLoadRevision + photoScopeVersion,
                    enabled = !managing && !refreshing,
                    showControls = viewerControls,
                    onSelected = { selectedId = it },
                    onToggleControls = { viewerControls = !viewerControls },
                    onShowInfo = { if (selected != null) showPhotoInfo = true },
                ) else key(selectedId, image) {
                    // A paired original/result can live outside the active filter.
                    // Keep it isolated instead of silently adding it to pager order.
                    image?.let { IosGalleryImageViewer(it, enabled = !managing && !refreshing,
                        showControls = viewerControls,
                        onToggleControls = { viewerControls = !viewerControls },
                        onShowInfo = { if (selected != null) showPhotoInfo = true }) }
                }
            }
            // A companion may live outside the active folder/favorites filter.
            // Do not silently expand previous/next navigation to the entire library.
            if (viewerControls) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), enabled = !managing && !refreshing && index > 0, onClick = { navigate(-1) }) { Text("이전 사진") }
                val companion = photos.firstOrNull { it.asset.assetId !in movedInViewer && it.captureId == selected?.captureId &&
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
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                when {
                                    selecting -> { selecting = false; batchSelection = emptySet() }
                                    recentView || defaultStorageView -> { recentView = false; defaultStorageView = false; folder = null }
                                    else -> onBack()
                                }
                            }, enabled = !managing, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기",
                                    tint = DDZColor.Primary, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = when {
                                    selecting -> "${selectedBatch.size}개 선택"
                                    recentView -> "최근 촬영"
                                    defaultStorageView -> "기본 저장"
                                    folder == null || folder == GALLERY_ROOT -> "갤러리"
                                    else -> folder!!.trimEnd('/').substringAfterLast('/')
                                },
                                modifier = Modifier.weight(1f),
                                style = DDZTypography.ScreenTitle,
                                color = DDZColor.Primary,
                                textAlign = TextAlign.Center,
                            )
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                                    .background(DDZColor.Card)
                                    .border(1.dp, DDZColor.Border, RoundedCornerShape(14.dp))
                                    .clickable(enabled = !managing && !refreshing) {
                                        favoritesOnly = !favoritesOnly
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (favoritesOnly) Icons.Default.GridView else Icons.Default.FavoriteBorder,
                                    contentDescription = if (favoritesOnly) "전체 보기" else "좋아요만 보기",
                                    tint = DDZColor.Primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(enabled = !refreshing && !managing, onClick = ::refresh) { Text("새로고침") }
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
                            if (folder != null && folder != GALLERY_ROOT) TextButton(enabled = !managing && !refreshing, onClick = {
                                folder = currentFolder?.let(GalleryFolderIndexPolicy::parentOf)
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
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(enabled = !managing && !refreshing, onClick = {
                                    openFolderAction(path, "create")
                                }) { Text("폴더 생성") }
                                if (path != GALLERY_ROOT) {
                                    if (!GalleryFolderNamePolicy.isOriginalFolder(path)) {
                                        TextButton(enabled = !managing && !refreshing, onClick = {
                                            openFolderAction(path, "move")
                                        }) { Text("폴더 이동") }
                                        TextButton(enabled = !managing && !refreshing, onClick = {
                                            openFolderAction(path, "rename")
                                        }) { Text("이름 변경") }
                                    }
                                    if (remember(path) { runCatching { GalleryFolderDeleteScope.source(path) }.isSuccess }) {
                                        TextButton(enabled = !managing && !refreshing, onClick = {
                                            openFolderAction(path, "delete")
                                        }) { Text("폴더 삭제") }
                                    }
                                }
                            }
                        }
                        loadError?.let { Text(it) }
                        operationMessage?.let { Text(it) }
                        if (!selecting && !defaultStorageView) folderListing?.pinnedOriginal?.let { original ->
                            Row(Modifier.fillMaxWidth().heightIn(min = DDZLayout.ListItem.CompactMinHeight)
                                .clickable(enabled = !managing && !refreshing, role = Role.Button) { folder = original.path }
                                .clearAndSetSemantics {
                                    contentDescription = "원본사진"
                                    if (original.directPhotoCount > 0) stateDescription = "${original.directPhotoCount}장"
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(DDZLayout.ListItem.CompactThumbnailSize)
                                    .clip(MaterialTheme.shapes.small).background(DDZColor.SurfaceSoft),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Folder, null, tint = DDZColor.Primary)
                                }
                                Column {
                                    Text("원본사진")
                                    if (original.directPhotoCount > 0) Text("${original.directPhotoCount}장", style = DDZTypography.Caption)
                                }
                            }
                        }
                        if (!selecting && !defaultStorageView && childFolders.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().heightIn(max = DDZLayout.ListItem.CompactMinHeight * 2)) {
                            items(childFolders, key = { it.path }) { summary ->
                                val label = summary.path.trimEnd('/').substringAfterLast('/')
                                val enabled = !managing && !refreshing
                                Row(Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    val counts = summary.counts
                                    val detail = buildList {
                                        val results = counts.totalPhotoCount - counts.originalPhotoCount
                                        if (results > 0) add("사진 ${results}장")
                                        if (counts.originalPhotoCount > 0) add("원본 ${counts.originalPhotoCount}장")
                                        if (counts.directChildFolderCount > 0) add("하위 폴더 ${counts.directChildFolderCount}개")
                                    }.joinToString(" · ")
                                    Row(Modifier.weight(1f).heightIn(min = DDZLayout.ListItem.CompactMinHeight)
                                        .clickable(enabled = enabled, role = Role.Button) { folder = summary.path }
                                        .clearAndSetSemantics {
                                            contentDescription = label
                                            if (detail.isNotEmpty()) stateDescription = detail
                                        }, verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val cover = summary.cover?.let { galleryThumbnail(it, repository, thumbnails, photoScopeVersion) }
                                        Box(Modifier.size(DDZLayout.ListItem.CompactThumbnailSize).clip(MaterialTheme.shapes.small)
                                            .background(DDZColor.SurfaceSoft), contentAlignment = Alignment.Center) {
                                            if (cover != null) Image(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            else Icon(Icons.Default.Folder, null, tint = DDZColor.IconMuted)
                                        }
                                        Column {
                                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (detail.isNotEmpty()) Text(detail, style = DDZTypography.Caption)
                                        }
                                    }
                                    if (!GalleryFolderNamePolicy.isOriginalFolder(summary.path)) {
                                        GalleryFolderRowMenu(summary.path, label, enabled, ::openFolderAction)
                                    }
                                }
                            }
                        }
                        if (!defaultStorageView && currentFolder != null && childFolders.isEmpty() && folderFilter != SavedGalleryFolderFilter.ALL) {
                            Text("조건에 맞는 하위 폴더가 없습니다.")
                        }
                        Text(message)
                        if (!selecting && selected != null && visible.any { it.asset.assetId == selectedId }) image?.let {
                            Image(it, "갤러리 사진 미리보기", Modifier.fillMaxWidth().height(100.dp).clickable { openViewer(selected.asset.assetId) },
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
                            val thumbnail = galleryThumbnail(entry, repository, thumbnails, photoScopeVersion)
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
                                    if (thumbnail != null) Image(thumbnail, "사진 썸네일", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosGalleryAnimatedPager(
    repository: PhotoKitCaptureMediaRepository,
    entries: List<SavedCaptureMedia>,
    selectedId: String?,
    imageLoadRevision: Int,
    enabled: Boolean,
    showControls: Boolean,
    onSelected: (String) -> Unit,
    onToggleControls: () -> Unit,
    onShowInfo: () -> Unit,
) {
    val initialPage = entries.indexOfFirst { it.asset.assetId == selectedId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { entries.size })
    var zoomedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val entryIds = entries.map { it.asset.assetId }
    val requestedId by rememberUpdatedState(selectedId)
    val selectPhoto by rememberUpdatedState(onSelected)

    LaunchedEffect(entryIds) {
        zoomedIds = zoomedIds.intersect(entryIds.toSet())
        if (entries.isNotEmpty() && pagerState.currentPage > entries.lastIndex) {
            pagerState.scrollToPage(entries.lastIndex)
        }
    }
    LaunchedEffect(selectedId, entryIds) {
        val target = entries.indexOfFirst { it.asset.assetId == selectedId }
        if (target >= 0 && target != pagerState.currentPage) pagerState.animateScrollToPage(target)
    }
    LaunchedEffect(pagerState, entryIds) {
        var previousId = entryIds.getOrNull(pagerState.settledPage)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val id = entryIds.getOrNull(page)
            // A button can select a new asset before the pager moves. The
            // outgoing page must never overwrite that explicit selection.
            if (id != null && id != previousId && previousId == requestedId) {
                selectPhoto(id)
            }
            previousId = id
        }
    }

    val currentId = entries.getOrNull(pagerState.currentPage)?.asset?.assetId
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { page -> entries[page].asset.assetId },
        beyondViewportPageCount = 1,
        userScrollEnabled = enabled && currentId !in zoomedIds,
    ) { page ->
        val entry = entries[page]
        val id = entry.asset.assetId
        val isCurrentPage = page == pagerState.settledPage
        val pageImage by produceState<ImageBitmap?>(
            initialValue = null,
            id,
            entry.availability,
            imageLoadRevision,
        ) {
            value = null
            if (entry.availability == CaptureMediaAvailability.PRESENT) {
                try {
                    val bytes = repository.readAsset(id)
                    currentCoroutineContext().ensureActive()
                    value = bytes?.takeIf { it.isNotEmpty() }?.let {
                        SkiaImage.makeFromEncoded(uprightIosImage(it)).toComposeImageBitmap()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // The active photo exposes the repository error and retry action.
                }
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            pageImage?.let {
                if (!isCurrentPage) {
                    Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else {
                    key(imageLoadRevision) {
                        IosGalleryImageViewer(
                            image = it,
                            enabled = enabled,
                            showControls = showControls,
                            onToggleControls = onToggleControls,
                            onShowInfo = onShowInfo,
                            pagerOwnsHorizontalSwipe = true,
                            onZoomedStateChange = { zoomed ->
                                zoomedIds = if (zoomed) zoomedIds + id else zoomedIds - id
                            },
                        )
                    }
                }
            } ?: Text(when (entry.availability) {
                CaptureMediaAvailability.PRESENT -> "사진 불러오는 중"
                CaptureMediaAvailability.INACCESSIBLE -> "사진 접근 권한이 없습니다."
                CaptureMediaAvailability.MISSING -> "저장된 사진을 찾을 수 없습니다."
            })
        }
    }
}

@Composable
private fun rememberMoveCatalog(
    repository: PhotoKitCaptureMediaRepository,
    photoScopeVersion: Int,
    retry: Int,
): Result<Pair<List<SavedCaptureMedia>, List<String>>>? {
    val catalog by produceState<Result<Pair<List<SavedCaptureMedia>, List<String>>>?>(
        null, repository, photoScopeVersion, retry,
    ) {
        value = null
        value = try {
            Result.success(repository.listSavedAssets() to repository.listGalleryFolders())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
    return catalog
}

@Composable
private fun galleryThumbnail(
    entry: SavedCaptureMedia,
    repository: PhotoKitCaptureMediaRepository,
    cache: MutableMap<String, ImageBitmap>,
    scopeVersion: Int,
): ImageBitmap? {
    val id = entry.asset.assetId
    return key(id, entry.availability, scopeVersion) {
        val bitmap by produceState<ImageBitmap?>(cache[id].takeIf { entry.availability == CaptureMediaAvailability.PRESENT }) {
            if (entry.availability != CaptureMediaAvailability.PRESENT) {
                cache.remove(id)
            } else if (value == null) {
                try {
                    value = repository.readThumbnail(id)?.let { SkiaImage.makeFromEncoded(uprightIosImage(it)).toComposeImageBitmap() }
                    value?.let {
                        if (cache.size >= 32) cache.remove(cache.keys.first())
                        cache[id] = it
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep the gallery tile visible when Photos cannot supply a preview.
                }
            }
        }
        bitmap
    }
}

@Composable
private fun GalleryFolderRowMenu(path: String, label: String, enabled: Boolean, onAction: (String, String) -> Unit) {
    var expanded by remember(path) { mutableStateOf(false) }
    fun choose(action: String) {
        expanded = false
        onAction(path, action)
    }
    Box {
        IconButton(enabled = enabled, onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "$label 폴더 관리")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("이름 변경") }, enabled = enabled, onClick = { choose("rename") })
            DropdownMenuItem(text = { Text("이동") }, enabled = enabled, onClick = { choose("move") })
            if (remember(path) { runCatching { GalleryFolderDeleteScope.source(path) }.isSuccess }) {
                DropdownMenuItem(text = { Text("삭제", color = DDZColor.Destructive) }, enabled = enabled,
                    onClick = { choose("delete") })
            }
        }
    }
}

@Composable
internal fun GalleryChoiceMenu(label: String, options: List<String>, enabled: Boolean, onSelected: (Int) -> Unit) {
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
