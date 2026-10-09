package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryChildFolder
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndex
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummary
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummaryPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import java.util.TimeZone

enum class GalleryTab(val label: String) {
    ALL("전체"),
    FOLDERS("폴더"),
    FAVORITES("즐겨찾기"),
}

/** Main gallery shares the same folder list and thumbnail primitives as subfolders. */
@Composable
internal fun LogGalleryHomeContent(
    selectedTab: GalleryTab,
    onTabChange: (GalleryTab) -> Unit,
    folderIndex: GalleryFolderIndex,
    summariesByPath: Map<String, GalleryFolderSummary>,
    allImages: List<MediaImageItem>,
    favoriteIds: Set<Long>,
    onOpenFolder: (String) -> Unit,
    onOpenPhoto: (List<MediaImageItem>, Int) -> Unit,
    onOpenOriginal: (String) -> Unit,
    onOpenRecentPhotos: () -> Unit,
    onManageFolder: (String, String) -> Unit,
    canManageFolders: Boolean,
    onCreateFolder: () -> Unit = {},
    selectedIds: Set<Long> = emptySet(),
    onLongPressPhoto: (MediaImageItem) -> Unit = {},
    onSelectPhoto: (MediaImageItem) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(DDZColor.SurfaceSoft, RoundedCornerShape(15.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GalleryTab.entries.forEach { tab ->
            val active = tab == selectedTab
            Box(
                modifier = Modifier.weight(1f)
                    .background(
                        if (active) DDZColor.Surface else DDZColor.SurfaceSoft,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onTabChange(tab) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    color = if (active) DDZColor.TextPrimary else DDZColor.TextSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }

    val root = folderIndex.relativePath
    val recent = allImages.filterNot { GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) }.take(9)
    val favorites = allImages.filter { it.id in favoriteIds }
    val directPhotos = allImages.filter { it.relativePath.trimEnd('/') == root.trimEnd('/') }
    // Summary IDs are MediaStore IDs, so resolve them against this same snapshot.
    val imagesById = remember(allImages) { allImages.associateBy { it.id.toString() } }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (selectedTab) {
            GalleryTab.ALL -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("최근 촬영", color = DDZColor.TextPrimary, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onOpenRecentPhotos, enabled = recent.isNotEmpty()) {
                            Text("전체보기")
                        }
                    }
                }
                if (recent.isEmpty()) {
                    item { GalleryEmptyText("저장된 사진이 없습니다.") }
                } else {
                    item {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val spacing = 5.dp
                            val photoSize = (maxWidth - spacing * 2) / 3
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing),
                            ) {
                                items(recent, key = { it.id }) { photo ->
                                    GalleryPhotoTile(
                                        photo, selectedIds, onLongPressPhoto, onSelectPhoto,
                                        modifier = Modifier.size(photoSize).clip(RoundedCornerShape(10.dp)),
                                    ) {
                                        onOpenPhoto(recent, recent.indexOfFirst { it.id == photo.id })
                                    }
                                }
                            }
                        }
                    }
                }
                item { GallerySectionTitle("저장 폴더", "${folderIndex.children.size}개", onCreateFolder) }
                if (folderIndex.children.isEmpty()) {
                    item { GalleryEmptyText("저장 폴더가 없습니다.") }
                } else {
                    items(folderIndex.children, key = { "folder-${it.relativePath}" }) { folder ->
                        val summary = summariesByPath[folder.relativePath]
                        GalleryFolderRow(folder, summary, summary?.coverImageId?.let(imagesById::get),
                            canManage = canManageFolders,
                            onManage = { action -> onManageFolder(folder.relativePath, action) },
                        ) {
                            onOpenFolder(folder.relativePath)
                        }
                    }
                }
            }
            GalleryTab.FOLDERS -> {
                item { GallerySectionTitle("저장 폴더", "${folderIndex.children.size}개", onCreateFolder) }
                if (folderIndex.children.isEmpty()) {
                    item { GalleryEmptyText("저장 폴더가 없습니다.") }
                } else {
                    items(folderIndex.children, key = { "folder-${it.relativePath}" }) { folder ->
                        val summary = summariesByPath[folder.relativePath]
                        GalleryFolderRow(folder, summary, summary?.coverImageId?.let(imagesById::get),
                            canManage = canManageFolders,
                            onManage = { action -> onManageFolder(folder.relativePath, action) },
                        ) {
                            onOpenFolder(folder.relativePath)
                        }
                    }
                }
                item { GallerySectionTitle("사진", "${directPhotos.size}장") }
                if (directPhotos.isEmpty()) {
                    item { GalleryEmptyText("DZlog 기본 위치에 저장된 사진이 없습니다.") }
                } else {
                    items(directPhotos.chunked(3)) { row ->
                        GalleryPhotoRow(row, selectedIds, onLongPressPhoto, onSelectPhoto) { photo ->
                            onOpenPhoto(directPhotos, directPhotos.indexOfFirst { it.id == photo.id })
                        }
                    }
                }
                if (folderIndex.directOriginalCount > 0) {
                    item {
                        GalleryOriginalRow(folderIndex.directOriginalCount) {
                            onOpenOriginal(root + "original/")
                        }
                    }
                }
            }
            GalleryTab.FAVORITES -> {
                item { GallerySectionTitle("즐겨찾기", "${favorites.size}장") }
                if (favorites.isEmpty()) {
                    item { GalleryEmptyText("즐겨찾기한 사진이 없습니다.") }
                } else {
                    items(favorites.chunked(3)) { row ->
                        GalleryPhotoRow(row, selectedIds, onLongPressPhoto, onSelectPhoto) { photo -> onOpenPhoto(favorites, favorites.indexOfFirst { it.id == photo.id }) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GallerySectionTitle(title: String, count: String? = null, onCreateFolder: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = DDZColor.TextPrimary, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (count != null && onCreateFolder == null) Text(count, color = DDZColor.TextSecondary)
            if (onCreateFolder != null) {
                TextButton(onClick = onCreateFolder) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Text(" 새 폴더")
                }
            }
        }
    }
}

@Composable
internal fun GalleryEmptyText(message: String) {
    Text(
        message,
        color = DDZColor.TextSecondary,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
internal fun GalleryFolderRow(
    folder: GalleryChildFolder,
    summary: GalleryFolderSummary?,
    coverImage: MediaImageItem?,
    canManage: Boolean,
    onManage: (String) -> Unit,
    onClick: () -> Unit,
) {
    var menuExpanded by remember(folder.relativePath) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DDZColor.SurfaceSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (coverImage != null) {
                DzThumbnail(coverImage.uri.toString())
            } else {
                Icon(Icons.Default.Folder, contentDescription = null, tint = DDZColor.Primary)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (summary == null) "집계 정보 없음" else
                    "사진 ${summary.totalPhotoCount} · 하위 ${summary.directChildFolderCount} · 최근 " +
                        GalleryFolderSummaryPolicy.compactDate(
                            epochMillis = summary.latestPhotoEpochMillis,
                            referenceMillis = System.currentTimeMillis(),
                            timeZone = TimeZone.getDefault(),
                        ),
                color = DDZColor.TextSecondary,
                fontSize = 12.sp,
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "${folder.name} 폴더 관리", tint = DDZColor.IconMuted)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("이름 변경") }, enabled = canManage,
                    onClick = { menuExpanded = false; onManage("rename") })
                DropdownMenuItem(text = { Text("이동") }, enabled = canManage,
                    onClick = { menuExpanded = false; onManage("move") })
                // Enable only after recursive deletion, permission and partial-failure handling are implemented.
                DropdownMenuItem(text = { Text("삭제") }, enabled = false, onClick = {})
            }
        }
    }
}

@Composable
internal fun GalleryOriginalRow(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = DDZColor.Primary)
        Text("원본사진", modifier = Modifier.weight(1f))
        Text("${count}장", color = DDZColor.TextSecondary)
        Icon(Icons.Default.ChevronRight, contentDescription = "원본사진 열기")
    }
}

@Composable
internal fun GalleryPhotoRow(
    photos: List<MediaImageItem>,
    selectedIds: Set<Long> = emptySet(),
    onLongPressPhoto: (MediaImageItem) -> Unit = {},
    onSelectPhoto: (MediaImageItem) -> Unit = {},
    onPhotoClick: (MediaImageItem) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        photos.forEach { photo ->
            GalleryPhotoTile(photo, selectedIds, onLongPressPhoto, onSelectPhoto,
                modifier = Modifier.weight(1f).aspectRatio(1f)) { onPhotoClick(photo) }
        }
        repeat(3 - photos.size) {
            Box(modifier = Modifier.weight(1f))
        }
    }
}

/** Same tap/selection behavior in the three-column grid and the recent strip. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryPhotoTile(
    photo: MediaImageItem,
    selectedIds: Set<Long>,
    onLongPressPhoto: (MediaImageItem) -> Unit,
    onSelectPhoto: (MediaImageItem) -> Unit,
    modifier: Modifier,
    onPhotoClick: () -> Unit,
) {
    Box(modifier = modifier.combinedClickable(
        onClick = { if (selectedIds.isNotEmpty()) onSelectPhoto(photo) else onPhotoClick() },
        onLongClick = { onLongPressPhoto(photo) },
    )) {
        DzThumbnail(photo.uri.toString())
        if (selectedIds.isNotEmpty()) {
            Checkbox(
                checked = photo.id in selectedIds,
                onCheckedChange = { onSelectPhoto(photo) },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
