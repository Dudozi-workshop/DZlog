package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderOperationPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.CancellationException

/** Same gallery folder cards and photo grid for photo and folder destination selection. */
@Composable
internal fun GalleryMoveDestinationPicker(
    allPhotos: List<MediaImageItem>,
    knownFolderPaths: List<String>,
    selectedIds: Set<Long>,
    initialPath: String,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    loadPhysicalChildren: suspend (String) -> List<String>,
    onCancel: () -> Unit,
    onChoose: (String) -> Unit,
    movingFolderPath: String? = null,
) {
    var path by rememberSaveable(initialPath, movingFolderPath) { mutableStateOf(initialPath) }
    var folderFilter by remember { mutableStateOf(GalleryFolderFilter.ALL) }
    var folderSort by remember { mutableStateOf(GalleryFolderSort.NAME) }
    var physicalPaths by remember(path) { mutableStateOf<List<String>>(emptyList()) }
    var childLoading by remember(path) { mutableStateOf(true) }
    var childError by remember(path) { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(path, retryKey) {
        childLoading = true
        childError = null
        try {
            physicalPaths = loadPhysicalChildren(path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            childError = e.message ?: "폴더를 읽지 못했습니다."
        }
        childLoading = false
    }

    val index = remember(path, allPhotos, knownFolderPaths, physicalPaths) {
        GalleryFolderIndexPolicy.index(path, allPhotos.map { it.relativePath }, knownFolderPaths + physicalPaths)
    }
    val folderChildren = remember(index.children, movingFolderPath) {
        index.children.filter { child ->
            movingFolderPath == null || (
                child.relativePath != movingFolderPath &&
                !child.relativePath.startsWith(movingFolderPath) &&
                !GalleryPhotoMovePolicy.isOriginalPath(child.relativePath)
            )
        }
    }
    val directPhotos = remember(path, allPhotos) {
        GalleryPhotoSort.NEWEST.sorted(allPhotos.filter {
            it.relativePath.trimEnd('/') == path.trimEnd('/') &&
                !GalleryPhotoMovePolicy.isOriginalPath(it.relativePath)
        })
    }
    val alreadyHere = movingFolderPath == null && allPhotos.any {
        it.id in selectedIds && it.relativePath.trimEnd('/') ==
            (if (GalleryPhotoMovePolicy.isOriginalPath(it.relativePath))
                path + "original" else path.trimEnd('/'))
    }
    val folderValidationError = movingFolderPath?.let { moving ->
        runCatching { GalleryFolderOperationPolicy.validateMove(moving, path) }
            .exceptionOrNull()?.message
            ?: if (path !in knownFolderPaths) "이 위치로는 이동할 수 없습니다." else null
    }
    val canChoose = if (movingFolderPath == null) selectedIds.isNotEmpty() && !alreadyHere
        else folderValidationError == null

    fun goBack() {
        val parent = GalleryFolderIndexPolicy.parentOf(path)
        if (parent == null) onCancel() else path = parent
    }

    Dialog(
        onDismissRequest = ::goBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // The footer is outside the scrolling content; system bars never overlap the action.
        Column(
            Modifier.fillMaxSize().background(DDZColor.Background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = DDZSpacing.screenPadding)
                .padding(bottom = 48.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = ::goBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "상위 폴더")
                    }
                    TextButton(onClick = onCancel) { Text("취소") }
                }
                Text(
                    "이동할 위치 선택",
                    modifier = Modifier.align(Alignment.Center),
                    style = DDZTypography.ScreenTitle,
                    color = DDZColor.TextPrimary,
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(bottom = DDZSpacing.itemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GalleryFolderIndexPolicy.breadcrumbs(path).forEachIndexed { i, crumb ->
                    if (i > 0) Text(" / ", color = DDZColor.TextSecondary)
                    Text(
                        crumb.label,
                        color = if (crumb.relativePath == path) DDZColor.TextPrimary else DDZColor.Primary,
                        modifier = Modifier.clickable { path = crumb.relativePath }.padding(vertical = 5.dp),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap),
                contentPadding = PaddingValues(bottom = DDZSpacing.sectionGap),
            ) {
                if (loading || childLoading) {
                    item { CircularProgressIndicator() }
                } else if (loadError != null || childError != null) {
                    item {
                        Text(loadError ?: childError.orEmpty(), color = DDZColor.Destructive)
                        TextButton(onClick = {
                            if (loadError != null) onRetry() else retryKey++
                        }) { Text("다시 시도") }
                    }
                } else {
                    if (path == GalleryFolderIndexPolicy.ROOT) {
                        item { GallerySectionTitle("최근 촬영") }
                        val recent = GalleryPhotoSort.NEWEST.sorted(allPhotos).take(9)
                        if (recent.isEmpty()) {
                            item { GalleryEmptyText("저장된 사진이 없습니다.") }
                        } else {
                            item {
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    items(recent, key = { it.id }) { photo ->
                                        Box(Modifier.fillParentMaxWidth(0.31f).aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))) {
                                            DzThumbnail(photo.uri.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { GallerySectionTitle(if (path == GalleryFolderIndexPolicy.ROOT) "저장 폴더" else "하위 폴더") }
                    if (path == GalleryFolderIndexPolicy.ROOT) {
                        item { GalleryFolderFilterControls(folderFilter, folderSort, true,
                            { folderFilter = it }, { folderSort = it }) }
                    }
                    val visibleFolders = filterGalleryFolders(folderChildren, emptyMap(), folderFilter, folderSort)
                    if (visibleFolders.isEmpty()) item { GalleryEmptyText("하위 폴더가 없습니다.") }
                    items(visibleFolders, key = { it.relativePath }) { child ->
                        val cover = allPhotos.firstOrNull {
                            it.relativePath.startsWith(child.relativePath) &&
                                !GalleryPhotoMovePolicy.isOriginalPath(it.relativePath)
                        }
                        GalleryFolderRow(
                            folder = child,
                            summary = null,
                            coverImage = cover,
                            canManage = false,
                            onManage = {},
                            showManagement = false,
                            detailOverride = if (child.totalImageCount > 0)
                                "사진 ${child.totalImageCount}장" else "빈 폴더",
                        ) { path = child.relativePath }
                    }
                    item {
                        GallerySectionTitle("이 폴더의 사진", count = "${directPhotos.size}장")
                    }
                    if (directPhotos.isEmpty()) {
                        item { GalleryEmptyText("저장된 사진이 없습니다.") }
                    } else {
                        items(directPhotos.take(9).chunked(3)) { photos ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                photos.forEach { photo ->
                                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))) {
                                        DzThumbnail(photo.uri.toString())
                                    }
                                }
                                repeat(3 - photos.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(DDZColor.Surface)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (alreadyHere) {
                    Text("선택한 사진이 이미 이 폴더에 있습니다.", color = DDZColor.TextSecondary,
                        style = DDZTypography.Caption)
                }
                if (movingFolderPath != null && folderValidationError != null) {
                    Text(folderValidationError, color = DDZColor.TextSecondary,
                        style = DDZTypography.Caption)
                }
                DDZButton(
                    text = "이 위치로 이동",
                    onClick = { onChoose(path) },
                    enabled = !loading && !childLoading && loadError == null &&
                        childError == null && canChoose,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
