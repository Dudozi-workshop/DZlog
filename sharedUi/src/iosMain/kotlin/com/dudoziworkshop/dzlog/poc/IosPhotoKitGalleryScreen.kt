package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.*
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage

private const val GALLERY_ROOT = "Pictures/DZlog/"

/** Browses the app's indexed Photos assets; logical folders never rename Photos resources. */
@Composable
internal fun IosPhotoKitGalleryScreen(repository: PhotoKitCaptureMediaRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var photos by remember { mutableStateOf<List<SavedCaptureMedia>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var message by remember { mutableStateOf("사진 확인 중") }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf<String?>(null) }
    var grid by remember { mutableStateOf(true) }
    var viewer by remember { mutableStateOf(false) }
    var managing by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var moveTarget by remember { mutableStateOf<String?>(null) }
    var destination by remember { mutableStateOf<String?>(null) }
    var operationMessage by remember { mutableStateOf<String?>(null) }
    val thumbnails = remember { LinkedHashMap<String, ImageBitmap>() }
    val visible = if (folder == null) photos else photos.filter { it.asset.target.relativePath == folder }
    val selected = photos.firstOrNull { it.asset.assetId == selectedId }
    val destinations = buildSet {
        add(GALLERY_ROOT)
        photos.forEach { entry ->
            var path = entry.asset.target.relativePath
            while (path.startsWith(GALLERY_ROOT) && path != GALLERY_ROOT) {
                add(path)
                path = path.trimEnd('/').substringBeforeLast('/') + "/"
            }
        }
    }.sorted()
    val currentFolder = folder
    val childFolders = if (currentFolder == null) emptyList() else photos.mapNotNull { entry ->
        val path = entry.asset.target.relativePath
        if (!path.startsWith(currentFolder) || path == currentFolder) null
        else currentFolder + path.removePrefix(currentFolder).substringBefore('/') + "/"
    }.distinct().sorted()

    fun refresh() {
        if (refreshing || managing) return
        refreshing = true
        scope.launch {
            try {
                photos = repository.listSavedAssets().asReversed()
                loadError = null
            } catch (error: Throwable) { loadError = "갤러리 조회 실패: ${error.message}" }
            finally { refreshing = false }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(photos, folder) {
        if (visible.none { it.asset.assetId == selectedId }) {
            val latestCapture = visible.firstOrNull()?.captureId
            selectedId = (visible.firstOrNull {
                it.captureId == latestCapture && it.asset.target.role == CaptureMediaRole.RESULT &&
                    it.availability == CaptureMediaAvailability.PRESENT
            } ?: visible.firstOrNull { it.availability == CaptureMediaAvailability.PRESENT }
                ?: visible.firstOrNull())?.asset?.assetId
        }
    }
    LaunchedEffect(selectedId, photos) {
        image = null
        val entry = photos.firstOrNull { it.asset.assetId == selectedId }
        if (entry == null) {
            message = if (photos.isEmpty()) "저장된 사진이 없습니다." else "폴더 안의 사진을 선택해 주세요."
            return@LaunchedEffect
        }
        if (entry.availability != CaptureMediaAvailability.PRESENT) {
            message = if (entry.availability == CaptureMediaAvailability.INACCESSIBLE)
                "사진 접근 권한이 없습니다." else "저장된 사진을 찾을 수 없습니다."
            return@LaunchedEffect
        }
        try {
            val bytes = repository.readAsset(entry.asset.assetId)
            val restored = bytes?.takeIf { it.isNotEmpty() }?.let { SkiaImage.makeFromEncoded(uprightIosImage(it)).toComposeImageBitmap() }
            if (selectedId == entry.asset.assetId) {
                image = restored
                message = if (restored == null) "사진 파일을 읽을 수 없습니다." else "사진 복원 완료"
            }
        } catch (error: Throwable) { if (selectedId == entry.asset.assetId) message = "사진 조회 실패: ${error.message}" }
    }

    deleteTarget?.let { targetId ->
        AlertDialog(onDismissRequest = { deleteTarget = null },
            title = { Text("사진 삭제") },
            text = { Text("현재 사진 1장을 iOS 사진 보관함에서 삭제합니다. 함께 촬영한 원본 또는 결과사진은 유지됩니다.") },
            confirmButton = { TextButton(onClick = {
                deleteTarget = null
                managing = true
                scope.launch {
                    try {
                        val removed = repository.deleteGalleryAsset(targetId)
                        operationMessage = if (removed) "사진을 삭제했습니다." else "삭제가 취소되었거나 사진을 삭제할 수 없습니다."
                        if (removed) {
                            thumbnails.remove(targetId)
                            image = null
                            viewer = false
                        }
                        photos = repository.listSavedAssets().asReversed()
                    } catch (error: Throwable) {
                        operationMessage = "삭제 결과 확인이 필요합니다: ${error.message}"
                    } finally { managing = false }
                }
            }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } })
    }
    moveTarget?.let { targetId ->
        val source = photos.firstOrNull { it.asset.assetId == targetId }?.asset?.target?.relativePath
        AlertDialog(onDismissRequest = { moveTarget = null },
            title = { Text("사진 이동") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("현재 사진 1장을 이동할 DZlog 폴더를 선택해 주세요. iOS 사진 앱의 앨범은 변경되지 않습니다.")
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
            confirmButton = { TextButton(enabled = destination != null && destination != source, onClick = {
                val path = destination ?: return@TextButton
                moveTarget = null
                managing = true
                scope.launch {
                    try {
                        repository.moveGalleryAsset(targetId, path)
                        photos = repository.listSavedAssets().asReversed()
                        folder = path
                        selectedId = targetId
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(enabled = !managing, onClick = { viewer = false }) { Text("사진 목록") }
                Text("사진 보기")
            }
            Text(selected?.asset?.target?.displayName.orEmpty())
            Text(message)
            operationMessage?.let { Text(it) }
            if (managing) Text("사진 처리 중")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !managing && !refreshing && selected?.availability == CaptureMediaAvailability.PRESENT,
                    onClick = { moveTarget = selectedId; destination = selected?.asset?.target?.relativePath }) { Text("사진 이동") }
                TextButton(enabled = !managing && !refreshing && selected?.availability == CaptureMediaAvailability.PRESENT,
                    onClick = { deleteTarget = selectedId }) { Text("사진 삭제") }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                image?.let { Image(it, "사진 확대 보기", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            }
            val entries = if (visible.any { it.asset.assetId == selectedId }) visible else photos
            val index = entries.indexOfFirst { it.asset.assetId == selectedId }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(enabled = !managing && index > 0, onClick = { selectedId = entries[index - 1].asset.assetId }) { Text("이전 사진") }
                val companion = photos.firstOrNull { it.captureId == selected?.captureId &&
                    it.asset.target.role != selected?.asset?.target?.role && it.availability == CaptureMediaAvailability.PRESENT }
                if (companion != null) Button(enabled = !managing, onClick = { selectedId = companion.asset.assetId }) {
                    Text(if (companion.asset.target.role == CaptureMediaRole.ORIGINAL) "원본 보기" else "결과 보기")
                }
                Button(enabled = !managing && index >= 0 && index < entries.lastIndex,
                    onClick = { selectedId = entries[index + 1].asset.assetId }) { Text("다음 사진") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack, enabled = !managing) { Text("뒤로가기") }
                Button(onClick = ::refresh, enabled = !refreshing && !managing) { Text("새로고침") }
                Text("갤러리")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { folder = if (folder == null) GALLERY_ROOT else null }) {
                    Text(if (folder == null) "폴더 보기" else "전체 사진")
                }
                TextButton(onClick = { grid = !grid }) { Text(if (grid) "목록 보기" else "그리드 보기") }
                if (folder != null && folder != GALLERY_ROOT) TextButton(onClick = {
                    folder = folder!!.trimEnd('/').substringBeforeLast('/') + "/"
                }) { Text("상위 폴더") }
            }
            Text("사진 ${visible.size}개")
            currentFolder?.let { Text("폴더: $it") }
            loadError?.let { Text(it) }
            operationMessage?.let { Text(it) }
            if (childFolders.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                items(childFolders) { path -> TextButton(onClick = { folder = path }) { Text(path.removePrefix(currentFolder.orEmpty()).trimEnd('/')) } }
            }
            Text(message)
            if (selected != null && visible.any { it.asset.assetId == selectedId }) image?.let {
                Image(it, "갤러리 사진 미리보기", Modifier.fillMaxWidth().height(160.dp).clickable { viewer = true },
                    contentScale = ContentScale.Fit)
            }
            if (grid) LazyVerticalGrid(GridCells.Fixed(3), modifier = Modifier.weight(1f),
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
                    Column(Modifier.fillMaxWidth().clickable { selectedId = entry.asset.assetId; viewer = true }) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(DDZColor.Surface), contentAlignment = Alignment.Center) {
                            if (thumbnail != null) Image(thumbnail!!, "사진 썸네일", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Text(if (entry.availability == CaptureMediaAvailability.PRESENT) "사진" else "접근 불가")
                        }
                        Text(if (entry.asset.target.role == CaptureMediaRole.RESULT) "결과" else "원본")
                        Text(entry.asset.target.displayName, maxLines = 1)
                    }
                }
            } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.asset.assetId }) { entry ->
                    Column(Modifier.fillMaxWidth().clickable { selectedId = entry.asset.assetId; viewer = true }.padding(8.dp)) {
                        Text("${entry.asset.target.displayName} · " + if (entry.asset.target.role == CaptureMediaRole.RESULT) "결과" else "원본")
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
