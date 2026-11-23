package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.data.media.*
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderNamePolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun IosGalleryMoveDestinationPicker(
    path: String,
    photos: List<SavedCaptureMedia>,
    folders: List<String>,
    selectedIds: Set<String>,
    loading: Boolean,
    error: String?,
    onNavigate: (String) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onChoose: () -> Unit,
    thumbnail: @Composable (SavedCaptureMedia) -> ImageBitmap?,
) {
    val root = path == GalleryFolderIndexPolicy.ROOT
    var filter by remember { mutableStateOf(SavedGalleryFolderFilter.ALL) }
    var sort by remember { mutableStateOf(SavedGalleryFolderSort.NAME) }
    val index = remember(photos, folders, path) {
        GalleryFolderIndexPolicy.index(path, photos.map { it.asset.target.relativePath }, folders)
    }
    val originalsFolder = GalleryFolderNamePolicy.isOriginalFolder(path)
    val inlineOriginals = GalleryFolderIndexPolicy.showsOriginalsDirectly(index)
    val recent = remember(photos) { SavedGalleryEntryPolicy.recentPreview(photos) }
    val direct = remember(photos, path, inlineOriginals) {
        val photoPath = if (inlineOriginals) path + "original/" else path
        SavedGalleryPhotoSort.NEWEST.sorted(photos.filter { it.asset.target.relativePath == photoPath })
    }
    val children = remember(path, folders, photos, filter, sort) {
        savedGalleryChildFolders(path, folders, photos, filter, sort)
            .filterNot { GalleryFolderNamePolicy.isOriginalFolder(it.path) }
    }
    val validation = remember(photos, selectedIds, path) {
        runCatching { SavedGalleryMovePolicy.destinations(photos, selectedIds, path, includeOriginals = false) }
    }
    fun back() {
        val parent = GalleryFolderIndexPolicy.parentOf(path)
        if (parent == null) onCancel() else onNavigate(parent)
    }
    Dialog(onDismissRequest = ::back, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "상위 폴더") }
                Text("이동할 위치 선택", style = DDZTypography.ScreenTitle)
                TextButton(onClick = onCancel) { Text("취소") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                GalleryFolderIndexPolicy.breadcrumbs(path).forEachIndexed { i, crumb ->
                    if (i > 0) Text("/")
                    TextButton(onClick = { onNavigate(crumb.relativePath) }) { Text(crumb.label) }
                }
            }
            key(path) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        loading -> item { CircularProgressIndicator() }
                        error != null -> item {
                            Text(error)
                            TextButton(onClick = onRetry) { Text("다시 시도") }
                        }
                        else -> {
                            if (root) {
                                item { Text("최근 촬영") }
                                item {
                                    if (recent.isEmpty()) Text("저장된 사진이 없습니다.")
                                    else LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(recent, key = { it.asset.assetId }) { photo ->
                                            DestinationThumbnail(photo, Modifier.fillParentMaxWidth(0.31f).aspectRatio(1f), thumbnail)
                                        }
                                    }
                                }
                            }
                            item { Text(if (root) "저장 폴더" else "하위 폴더") }
                            if (root) item {
                                Row {
                                    GalleryChoiceMenu("폴더 · ${filter.label}", SavedGalleryFolderFilter.entries.map { it.label }, true) {
                                        filter = SavedGalleryFolderFilter.entries[it]
                                    }
                                    GalleryChoiceMenu("폴더순 · ${sort.label}", SavedGalleryFolderSort.entries.map { it.label }, true) {
                                        sort = SavedGalleryFolderSort.entries[it]
                                    }
                                }
                            }
                            if (children.isEmpty()) item { Text("하위 폴더가 없습니다.") }
                            items(children, key = { it.path }) { child ->
                                val cover = remember(photos, child.path) { photos.firstOrNull {
                                    it.asset.target.relativePath.startsWith(child.path) &&
                                        !GalleryFolderNamePolicy.isOriginalFolder(it.asset.target.relativePath)
                                } }
                                Surface(onClick = { onNavigate(child.path) }, color = DDZColor.Surface,
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        if (cover != null) DestinationThumbnail(cover, Modifier.size(40.dp), thumbnail)
                                        Column {
                                            Text(child.path.removePrefix(path).trimEnd('/'))
                                            Text(if (child.photoCount == 0) "빈 폴더" else "사진 ${child.photoCount}장",
                                                style = DDZTypography.Caption)
                                        }
                                    }
                                }
                            }
                            if (!inlineOriginals && !originalsFolder && index.directOriginalCount > 0) item {
                                TextButton(onClick = { onNavigate(path + "original/") }) {
                                    Text("원본사진 ${index.directOriginalCount}장")
                                }
                            }
                            item { Text((if (inlineOriginals || originalsFolder) "원본사진" else "이 폴더의 사진") + " · ${direct.size}장") }
                            if (direct.isEmpty()) item { Text("저장된 사진이 없습니다.") }
                            items(direct.chunked(3)) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { DestinationThumbnail(it, Modifier.weight(1f).aspectRatio(1f), thumbnail) }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
            if (!loading && error == null) {
                val reason = if (originalsFolder) "원본사진 폴더는 이동 위치로 선택할 수 없습니다."
                    else validation.exceptionOrNull()?.message
                reason?.let { Text(it, style = DDZTypography.Caption) }
            }
            Button(onClick = onChoose, enabled = !loading && error == null && !originalsFolder && validation.isSuccess,
                modifier = Modifier.fillMaxWidth()) { Text("이 위치로 이동") }
        }
    }
}

@Composable
private fun DestinationThumbnail(
    entry: SavedCaptureMedia,
    modifier: Modifier,
    thumbnail: @Composable (SavedCaptureMedia) -> ImageBitmap?,
) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(DDZColor.Surface), contentAlignment = Alignment.Center) {
        val image = thumbnail(entry)
        if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(if (entry.availability == CaptureMediaAvailability.PRESENT) "사진" else "접근 불가", style = DDZTypography.Caption)
    }
}
