package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

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
    selectionActive: Boolean = selectedIds.isNotEmpty(),
    onLongPressPhoto: (MediaImageItem) -> Unit = {},
    onSelectPhoto: (MediaImageItem) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(DDZColor.SurfaceSoft, MaterialTheme.shapes.medium)
            .padding(DDZSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(DDZSpacing.xs),
    ) {
        GalleryTab.entries.forEach { tab ->
            val active = tab == selectedTab
            Box(
                modifier = Modifier.weight(1f)
                    .background(
                        if (active) DDZColor.Surface else DDZColor.SurfaceSoft,
                        MaterialTheme.shapes.small,
                    )
                    .clickable(enabled = !selectionActive) { onTabChange(tab) }
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
        verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap),
        contentPadding = PaddingValues(bottom = DDZSpacing.sectionGap),
    ) {
        when (selectedTab) {
            GalleryTab.ALL -> {
                item {
                    GallerySectionTitle(
                        title = "최근 촬영",
                        actionLabel = "전체보기",
                        onAction = onOpenRecentPhotos,
                        actionEnabled = recent.isNotEmpty() && !selectionActive,
                    )
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
                                        selectionActive = selectionActive,
                                        modifier = Modifier.size(photoSize).clip(RoundedCornerShape(10.dp)),
                                    ) {
                                        onOpenPhoto(recent, recent.indexOfFirst { it.id == photo.id })
                                    }
                                }
                            }
                        }
                    }
                }
                item { GallerySectionTitle(
                    title = "저장 폴더",
                    count = "${folderIndex.children.size}개",
                    actionLabel = "새 폴더",
                    onAction = onCreateFolder,
                    showAddIcon = true,
                ) }
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
                item { GallerySectionTitle(
                    title = "저장 폴더",
                    count = "${folderIndex.children.size}개",
                    actionLabel = "새 폴더",
                    onAction = onCreateFolder,
                    showAddIcon = true,
                ) }
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
                        GalleryPhotoRow(row, selectedIds, onLongPressPhoto, onSelectPhoto, selectionActive = selectionActive) { photo ->
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
                        GalleryPhotoRow(row, selectedIds, onLongPressPhoto, onSelectPhoto, selectionActive = selectionActive) { photo -> onOpenPhoto(favorites, favorites.indexOfFirst { it.id == photo.id }) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GallerySectionTitle(
    title: String,
    count: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
    showAddIcon: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = DDZSpacing.screenPadding, bottom = DDZSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = DDZColor.TextPrimary, style = DDZTypography.CompactSectionTitle)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (count != null && onAction == null) {
                Text(count, color = DDZColor.TextSecondary, style = DDZTypography.Body)
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction, enabled = actionEnabled) {
                    if (showAddIcon) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    }
                    Text(
                        actionLabel,
                        modifier = if (showAddIcon) Modifier.padding(start = DDZSpacing.xs) else Modifier,
                        style = DDZTypography.TextAction,
                    )
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
            .background(DDZColor.Surface, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = DDZLayout.ListItem.CompactMinHeight)
            .padding(horizontal = DDZSpacing.controlGap, vertical = DDZSpacing.itemGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DDZSpacing.compactCardContentGap),
    ) {
        Box(
            modifier = Modifier.size(DDZLayout.ListItem.CompactThumbnailSize)
                .clip(MaterialTheme.shapes.small)
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
            val details = if (summary == null) "집계 정보 없음" else buildList {
                val results = summary.totalPhotoCount - summary.originalPhotoCount
                if (results > 0) add("사진 ${results}장")
                if (summary.originalPhotoCount > 0) add("원본 ${summary.originalPhotoCount}장")
                if (summary.directChildFolderCount > 0) add("하위 폴더 ${summary.directChildFolderCount}개")
            }.joinToString(" · ")
            if (details.isNotEmpty()) Text(details, color = DDZColor.TextSecondary, style = DDZTypography.Secondary)
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "${folder.name} 폴더 관리",
                    modifier = Modifier.size(DDZLayout.Icon.Small),
                    tint = DDZColor.IconMuted,
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("이름 변경") }, enabled = canManage,
                    onClick = { menuExpanded = false; onManage("rename") })
                DropdownMenuItem(text = { Text("이동") }, enabled = canManage,
                    onClick = { menuExpanded = false; onManage("move") })
                DropdownMenuItem(text = { Text("삭제", color = DDZColor.Destructive) }, enabled = canManage,
                    onClick = { menuExpanded = false; onManage("delete") })
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
    selectionActive: Boolean = selectedIds.isNotEmpty(),
    onPhotoClick: (MediaImageItem) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        photos.forEach { photo ->
            GalleryPhotoTile(photo, selectedIds, onLongPressPhoto, onSelectPhoto,
                selectionActive = selectionActive,
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
    selectionActive: Boolean,
    modifier: Modifier,
    onPhotoClick: () -> Unit,
) {
    Box(modifier = modifier.combinedClickable(
        onClick = { if (selectionActive) onSelectPhoto(photo) else onPhotoClick() },
        onLongClick = { onLongPressPhoto(photo) },
    )) {
        DzThumbnail(photo.uri.toString())
        if (selectionActive) {
            Checkbox(
                checked = photo.id in selectedIds,
                onCheckedChange = { onSelectPhoto(photo) },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
