package com.dudoziworkshop.dzlog.ui.log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.dudoziworkshop.dzlog.data.favorites.FavoritesProvider
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.data.mediastore.GalleryFolderStorage
import com.dudoziworkshop.dzlog.data.mediastore.GallerySnapshotMemory
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndex
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
    val folderStorage = remember(context) { GalleryFolderStorage(context.applicationContext) }
    val favoriteIds by remember(context) { FavoritesProvider.repo(context) }.favoriteIdsFlow.collectAsState(initial = emptySet())
    var selectedTab by remember { mutableStateOf(GalleryTab.ALL) }
    val cached = remember(relativePath) { GallerySnapshotMemory.cache.get(relativePath) }
    var allPhotos by remember(relativePath) { mutableStateOf(cached?.allPhotos ?: emptyList()) }
    var index by remember(relativePath) { mutableStateOf<GalleryFolderIndex?>(cached?.index) }
    var photos by remember(relativePath) { mutableStateOf(cached?.directPhotos ?: emptyList()) }
    var error by remember(relativePath) { mutableStateOf<String?>(null) }
    var loading by remember(relativePath) { mutableStateOf(cached == null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var manageAction by remember { mutableStateOf<String?>(null) }
    var renameName by remember { mutableStateOf("") }
    var moveTarget by remember { mutableStateOf(GalleryFolderIndexPolicy.ROOT) }
    var folderOperationError by remember { mutableStateOf<String?>(null) }
    var connected by remember { mutableStateOf(folderStorage.isConnected()) }

    var reloadKey by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching { folderStorage.connect(uri) }
                .onSuccess { connected = true; folderOperationError = null; GallerySnapshotMemory.cache.invalidate(relativePath); reloadKey++ }
                .onFailure { folderOperationError = it.message ?: "폴더 연결 실패" }
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
                val folders = if (folderStorage.isConnected())
                    runCatching { folderStorage.listImmediateFolderPaths(relativePath) }.getOrDefault(emptyList())
                    else emptyList()
                reader.loadGallerySnapshot(relativePath, existingFolderPaths = folders)
            }
        }
        result.onSuccess { snapshot ->
            GallerySnapshotMemory.cache.put(relativePath, snapshot)
            index = snapshot.index
            photos = snapshot.directPhotos
            if (relativePath == GalleryFolderIndexPolicy.ROOT) allPhotos = snapshot.allPhotos
            error = null
        }.onFailure { if (index == null) error = "목록을 불러오지 못했습니다." }
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
                        modifier = Modifier.clickable {
                            if (crumb.relativePath != relativePath) onOpenFolder(crumb.relativePath)
                        }.padding(vertical = 5.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    folderOperationError = null
                    if (connected) showCreateDialog = true else folderPicker.launch(null)
                },
            ) { Text("새 폴더") }
            if (!root && connected) {
                OutlinedButton(onClick = {
                    renameName = relativePath.trimEnd('/').substringAfterLast('/')
                    manageAction = "rename"
                }) { Text("이름 변경") }
                OutlinedButton(onClick = {
                    moveTarget = GalleryFolderIndexPolicy.ROOT
                    manageAction = "move"
                }) { Text("이동") }
            }
        }
        folderOperationError?.let { message ->
            Text(message, color = DDZColor.Destructive)
        }
        if (loading && index == null) {
            GalleryLoadingSkeleton()
        } else if (error != null) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("사진 목록을 표시할 수 없습니다.", color = DDZColor.TextSecondary)
                OutlinedButton(onClick = { reloadKey++ }) { Text("다시 시도") }
            }
        } else if (root && index != null) {
            LogGalleryHomeContent(
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                folderIndex = requireNotNull(index),
                allImages = allPhotos,
                favoriteIds = favoriteIds,
                onOpenFolder = onOpenFolder,
                onOpenPhoto = onOpenPhoto,
                onOpenOriginal = onOpenOriginal,
            )
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
                        GalleryPhotoRow(rowPhotos) { photo ->
                            onOpenPhoto(photos, photos.indexOfFirst { it.id == photo.id })
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
        if (manageAction != null) {
            AlertDialog(
                onDismissRequest = { manageAction = null },
                title = { Text(if (manageAction == "rename") "폴더 이름 변경" else "폴더 이동") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (manageAction == "rename") {
                            OutlinedTextField(
                                value = renameName,
                                onValueChange = { renameName = it },
                                label = { Text("새 폴더 이름") },
                                singleLine = true,
                            )
                        } else {
                            Text("이동할 상위 폴더를 선택하세요.")
                            GalleryFolderIndexPolicy.breadcrumbs(relativePath).dropLast(1)
                                .forEach { crumb ->
                                    OutlinedButton(onClick = { moveTarget = crumb.relativePath }) {
                                        Text(if (moveTarget == crumb.relativePath)
                                            "✓ ${crumb.label}" else crumb.label)
                                    }
                                }
                        }
                        Text(
                            "폴더 내부 사진·원본사진도 함께 변경됩니다. 촬영 저장설정은 자동 변경되지 않으므로, 설정된 저장경로가 영향받을 수 있습니다.",
                            color = DDZColor.TextSecondary,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val action = manageAction
                        runCatching {
                            if (action == "rename")
                                folderStorage.renameFolder(relativePath, renameName)
                            else folderStorage.moveFolder(relativePath, moveTarget)
                        }.onSuccess { newPath ->
                            manageAction = null
                            folderOperationError = null
                            GallerySnapshotMemory.cache.invalidate()
                            onOpenFolder(newPath)
                            reloadKey++
                        }.onFailure { folderOperationError = it.message ?: "폴더 관리 실패" }
                    }) { Text("변경") }
                },
                dismissButton = {
                    TextButton(onClick = { manageAction = null }) { Text("취소") }
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
                        runCatching { folderStorage.createFolder(relativePath, newFolderName) }
                            .onSuccess {
                                showCreateDialog = false
                                newFolderName = ""
                                folderOperationError = null
                                GallerySnapshotMemory.cache.invalidate(relativePath)
                                reloadKey++
                            }
                            .onFailure { folderOperationError = it.message ?: "폴더 생성 실패" }
                    }) { Text("생성") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("취소") }
                },
            )
        }
    }
}
