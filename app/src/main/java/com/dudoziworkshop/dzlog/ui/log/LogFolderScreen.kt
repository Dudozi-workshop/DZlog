package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndex
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One screen for every folder depth; the route state is the actual relative path. */
@Composable
fun LogFolderScreen(
    relativePath: String,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPhoto: (List<MediaImageItem>, Int) -> Unit,
    onOpenOriginal: (String) -> Unit,
) {
    val context = LocalContext.current
    val reader = remember(context) { DzlogMediaStoreReader(context.contentResolver) }
    var index by remember(relativePath) { mutableStateOf<GalleryFolderIndex?>(null) }
    var photos by remember(relativePath) { mutableStateOf<List<MediaImageItem>>(emptyList()) }
    var error by remember(relativePath) { mutableStateOf<String?>(null) }
    var loading by remember(relativePath) { mutableStateOf(true) }

    LaunchedEffect(reader, relativePath) {
        loading = true
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val folder = reader.loadFolderIndex(relativePath)
                folder to reader.loadImages(relativePath)
            }
        }
        result.onSuccess { (folder, loaded) ->
            index = folder
            photos = loaded
            error = null
        }.onFailure { error = it.message ?: "폴더를 불러오지 못했습니다." }
        loading = false
    }

    val root = relativePath == GalleryFolderIndexPolicy.ROOT
    val title = if (root) "갤러리" else relativePath.trimEnd('/').substringAfterLast('/')
    Column(modifier = Modifier.dzScreen().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DDZColor.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(relativePath.removePrefix(GalleryFolderIndexPolicy.ROOT).ifBlank { "Pictures / DZlog" },
                    color = DDZColor.TextSecondary, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(20.dp))
        } else if (error != null) {
            Text("불러오기 오류: $error", modifier = Modifier.padding(16.dp))
        } else {
            val current = index
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (current != null && current.children.isNotEmpty()) {
                    item { Text(if (root) "저장 폴더" else "하위 폴더", fontWeight = FontWeight.SemiBold) }
                    items(current.children, key = { it.relativePath }) { folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onOpenFolder(folder.relativePath) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = DDZColor.Primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${folder.totalImageCount}장", color = DDZColor.TextSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = "열기")
                        }
                    }
                }
                item {
                    Text("사진 ${photos.size}장", fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp))
                }
                if (photos.isEmpty()) {
                    item { Text("이 폴더에 저장된 사진이 없습니다.", color = DDZColor.TextSecondary) }
                } else {
                    items(photos.chunked(3)) { rowPhotos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            rowPhotos.forEach { photo ->
                                val position = photos.indexOf(photo)
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                        .clickable { onOpenPhoto(photos, position) }
                                ) {
                                    DzThumbnail(photo.uri.toString())
                                }
                            }
                            repeat(3 - rowPhotos.size) {
                                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (current != null && current.directOriginalCount > 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onOpenOriginal(relativePath + "original/") }
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
}
