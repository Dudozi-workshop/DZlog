package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.CancellationException

/** Browse a destination independently of the gallery route and its selection state. */
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
) {
    var path by rememberSaveable { mutableStateOf(initialPath) }
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
    val preview = remember(path, allPhotos) {
        GalleryPhotoSort.NEWEST.sorted(allPhotos.filter {
            it.relativePath.trimEnd('/') == path.trimEnd('/') && !GalleryPhotoMovePolicy.isOriginalPath(it.relativePath)
        }).take(3)
    }
    val alreadyHere = allPhotos.any { it.id in selectedIds &&
        it.relativePath.trimEnd('/') == (if (GalleryPhotoMovePolicy.isOriginalPath(it.relativePath))
            path + "original" else path.trimEnd('/')) }
    fun goBack() {
        val parent = GalleryFolderIndexPolicy.parentOf(path)
        if (parent == null) onCancel() else path = parent
    }
    Dialog(onDismissRequest = ::goBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.dzScreen().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::goBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "상위 폴더")
                }
                Text("이동할 폴더", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onCancel) { Text("취소") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                GalleryFolderIndexPolicy.breadcrumbs(path).forEachIndexed { i, crumb ->
                    if (i > 0) Text(" / ", color = DDZColor.TextSecondary)
                    Text(crumb.label, color = DDZColor.Primary,
                        modifier = Modifier.clickable { path = crumb.relativePath }.padding(vertical = 4.dp))
                }
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading || childLoading) {
                    item { CircularProgressIndicator() }
                } else if (loadError != null || childError != null) {
                    item {
                        Text(loadError ?: childError.orEmpty(), color = DDZColor.Destructive)
                        TextButton(onClick = { if (loadError != null) onRetry() else retryKey++ }) { Text("다시 시도") }
                    }
                } else {
                    item { Text("이 폴더의 사진", fontWeight = FontWeight.SemiBold) }
                    item {
                        if (preview.isEmpty()) GalleryEmptyText("이 폴더에 저장된 사진이 없습니다.")
                        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            preview.forEach { photo ->
                                Box(Modifier.weight(1f).aspectRatio(1f)) { DzThumbnail(photo.uri.toString()) }
                            }
                            repeat(3 - preview.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                    item { Text("하위 폴더", fontWeight = FontWeight.SemiBold) }
                    if (index.children.isEmpty()) item { GalleryEmptyText("하위 폴더가 없습니다.") }
                    items(index.children, key = { it.relativePath }) { child ->
                        Row(Modifier.fillMaxWidth().clickable { path = child.relativePath }.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = DDZColor.Primary)
                            Text(child.name, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = "폴더 열기")
                        }
                    }
                }
            }
            if (alreadyHere) Text("선택한 사진이 이미 이 폴더에 있습니다.", color = DDZColor.TextSecondary)
            Button(onClick = { onChoose(path) }, enabled = !loading && !childLoading && loadError == null &&
                childError == null && !alreadyHere && selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(vertical = 8.dp)) { Text("이 위치로 이동") }
        }
    }
}
