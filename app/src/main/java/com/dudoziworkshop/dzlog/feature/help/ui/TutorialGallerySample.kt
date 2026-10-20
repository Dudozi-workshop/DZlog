package com.dudoziworkshop.dzlog.feature.help.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummary
import com.dudoziworkshop.dzlog.ui.log.GalleryFolderRow
import com.dudoziworkshop.dzlog.ui.log.GalleryOriginalRow
import com.dudoziworkshop.dzlog.ui.log.GalleryPathChangeArrow
import com.dudoziworkshop.dzlog.ui.log.GalleryPathPreviewCard
import com.dudoziworkshop.dzlog.ui.log.GalleryPhotoRow
import com.dudoziworkshop.dzlog.ui.log.GallerySectionTitle
import com.dudoziworkshop.dzlog.ui.log.GalleryTab
import com.dudoziworkshop.dzlog.ui.log.LogGalleryHomeContent
import com.dudoziworkshop.dzlog.ui.log.DzThumbnail
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/**
 * Demonstration records reference bundled resources only. No MediaStore, SAF, folder repository,
 * user favorites or camera preferences are queried or modified here.
 */
internal object TutorialGallerySample {
    const val ROOT = "Pictures/DZlog/"
    val folders = listOf(ROOT + "단이 사진/", ROOT + "여행 기록/", ROOT + "업무 기록/",
        ROOT + "단이 사진/산책/")
    fun photos(packageName: String): List<MediaImageItem> {
        val imageIds = listOf(R.drawable.onboarding_gallery_photo, R.drawable.onboarding_hero_photo,
            R.drawable.onboarding_starter_photo)
        val directories = listOf(ROOT, ROOT + "단이 사진/", ROOT + "여행 기록/",
            ROOT + "단이 사진/산책/", ROOT + "업무 기록/", ROOT + "단이 사진/original/")
        return directories.mapIndexed { index, directory ->
            MediaImageItem(
                id = -(index + 100L),
                uri = Uri.parse("android.resource://" + packageName + "/" + imageIds[index % imageIds.size]),
                displayName = "DZlog_예시_" + (index + 1) + ".jpg",
                relativePath = directory,
                dateAddedSeconds = 1780000000L - index * 60L,
            )
        }
    }
}

/**
 * Real gallery presentation primitives backed by the same in-memory tutorial dataset.
 * The guide's pointer shield consumes all touches; all callbacks here are also inert.
 */
@Composable
internal fun TutorialGallerySampleScreen(location: TutorialLocation) {
    val context = LocalContext.current
    val images = remember(context.packageName) { TutorialGallerySample.photos(context.packageName) }
    val folders = TutorialGallerySample.folders
    val root = TutorialGallerySample.ROOT
    val index = remember(images) {
        GalleryFolderIndexPolicy.index(root, images.map { it.relativePath }, folders)
    }
    val summaries = remember(images) {
        index.children.associate { folder ->
            val matches = images.filter { it.relativePath.startsWith(folder.relativePath) }
            folder.relativePath to GalleryFolderSummary(
                totalPhotoCount = matches.size,
                directPhotoCount = matches.count { it.relativePath == folder.relativePath },
                originalPhotoCount = matches.count { it.relativePath.endsWith("original/") },
                directChildFolderCount = folders.count {
                    it.startsWith(folder.relativePath) && it != folder.relativePath &&
                        !it.removePrefix(folder.relativePath).trimEnd('/').contains('/')
                },
                latestPhotoEpochMillis = matches.maxOfOrNull { it.dateAddedSeconds * 1000L },
                coverImageId = matches.firstOrNull()?.id?.toString(),
            )
        }
    }
    val session = location.sessionId
    val step = location.pageId
    val isDestination = step.endsWith("/destination")
    val isConfirmation = step in setOf("folders/rename", "folders/move", "folders/delete",
        "photos/move", "photos/delete")
    val isViewer = session == "viewer"
    val showFolder = session == "folders" || step == "browse"
    val isPhotoSession = session == "photos"
    val selected = if (isPhotoSession) setOf(images[1].id) else emptySet()
    var selectedTab by remember { mutableStateOf(GalleryTab.ALL) }

    Column(
        Modifier.fillMaxSize().background(DDZColor.Background).statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                when {
                    isDestination -> "이동할 위치 선택"
                    isViewer -> "사진 보기"
                    isPhotoSession -> "최근 촬영"
                    showFolder -> "단이 사진"
                    else -> "갤러리"
                },
                Modifier.weight(1f), style = DDZTypography.ScreenTitle,
                color = DDZColor.TextPrimary,
            )
            Text(
                "안내 예시", style = DDZTypography.Caption,
                color = DDZColor.TextSecondary,
            )
        }

        when {
            isDestination -> {
                DestinationExample(images, folders, step == "folders/destination",
                    Modifier.weight(1f).tutorialTarget("guide.dialog"))
            }
            isViewer -> {
                Box(Modifier.weight(1f).tutorialTarget("gallery.content")) {
                    DzThumbnail(images[1].uri.toString())
                }
                Row(
                    Modifier.fillMaxWidth().tutorialTarget("gallery.viewer-actions"),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text("즐겨찾기", color = DDZColor.TextPrimary)
                    Text("공유", color = DDZColor.TextPrimary)
                    Text("정보", color = DDZColor.TextPrimary)
                    Text("삭제", color = DDZColor.TextPrimary)
                }
            }
            !showFolder && !isPhotoSession -> {
                Box(Modifier.weight(1f).tutorialTarget("gallery.content")) {
                    LogGalleryHomeContent(
                        selectedTab = selectedTab,
                        onTabChange = { selectedTab = it },
                        folderIndex = index,
                        summariesByPath = summaries,
                        allImages = images,
                        favoriteIds = emptySet(),
                        showCounterBadges = false,
                        onOpenFolder = {},
                        onOpenPhoto = { _, _ -> },
                        onOpenOriginal = {},
                        onOpenRecentPhotos = {},
                        onOpenDefaultPhotos = {},
                        onSelectDefaultPhotos = {},
                        onManageFolder = { _, _ -> },
                        canManageFolders = true,
                        onCreateFolder = {},
                    )
                }
            }
            else -> {
                val folderPath = TutorialGallerySample.ROOT + "단이 사진/"
                Row(
                    Modifier.fillMaxWidth().tutorialTarget("gallery.breadcrumbs"),
                ) {
                    GalleryFolderIndexPolicy.breadcrumbs(folderPath).forEachIndexed { n, crumb ->
                        if (n > 0) Text(" / ", color = DDZColor.TextSecondary)
                        Text(crumb.label, color = DDZColor.PrimaryDark,
                            style = DDZTypography.Caption)
                    }
                }
                Box(Modifier.weight(1f).tutorialTarget("gallery.content")) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (showFolder) {
                            item { GallerySectionTitle("하위 폴더") }
                            item {
                                val child = GalleryFolderIndexPolicy.index(folderPath,
                                    images.map { it.relativePath }, folders).children.first()
                                GalleryFolderRow(child, null, null, canManage = true,
                                    onManage = {}, onClick = {})
                            }
                            item {
                                GalleryOriginalRow(1, onClick = {})
                            }
                        }
                        item { GallerySectionTitle(if (isPhotoSession) "최근 촬영" else "이 폴더의 사진") }
                        items(images.chunked(3)) { row ->
                            GalleryPhotoRow(row, selectedIds = selected, selectionActive = isPhotoSession,
                                showCounterBadges = false, onPhotoClick = {})
                        }
                    }
                }
                if (isPhotoSession) {
                    Row(
                        Modifier.fillMaxWidth().tutorialTarget("gallery.selection-actions"),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        listOf("이동", "공유", "삭제").forEachIndexed { index, title ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    when (index) {
                                        0 -> Icons.Default.DriveFileMove
                                        1 -> Icons.Default.Share
                                        else -> Icons.Default.Delete
                                    },
                                    contentDescription = null,
                                    tint = DDZColor.TextPrimary,
                                )
                                Text(title, color = DDZColor.TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        if (isConfirmation) {
            GalleryOperationPreview(step, Modifier.fillMaxWidth().tutorialTarget("guide.dialog"))
        }
    }
}

@Composable
private fun DestinationExample(
    images: List<MediaImageItem>,
    folderPaths: List<String>,
    movingFolder: Boolean,
    modifier: Modifier = Modifier,
) {
    val path = TutorialGallerySample.ROOT + "단이 사진/"
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("DZlog / 단이 사진", color = DDZColor.PrimaryDark, style = DDZTypography.Body)
        }
        item { GallerySectionTitle("하위 폴더") }
        val descendants = GalleryFolderIndexPolicy.index(path, images.map { it.relativePath }, folderPaths).children
        items(descendants) { folder ->
            GalleryFolderRow(folder, null, null, canManage = false, showManagement = false,
                onManage = {}, onClick = {})
        }
        item { GallerySectionTitle("이 폴더의 사진") }
        val photos = images.filter { it.relativePath == path }
        items(photos.chunked(3)) { row ->
            GalleryPhotoRow(row, showCounterBadges = false, onPhotoClick = {})
        }
        item {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark)) {
                Text("이 위치로 이동")
            }
        }
        if (movingFolder) item {
            Text("현재 폴더와 하위 폴더는 이동 대상으로 선택할 수 없어요.",
                style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        }
    }
}

@Composable
private fun GalleryOperationPreview(step: String, modifier: Modifier = Modifier) {
    val isFolder = step.startsWith("folders/")
    val oldPath = "DZlog / 단이 사진"
    val newPath = when (step) {
        "folders/rename" -> "DZlog / 단이 기록"
        "folders/move" -> "DZlog / 업무 기록 / 단이 사진"
        "photos/move" -> "DZlog / 업무 기록"
        else -> oldPath
    }
    Surface(modifier, color = DDZColor.Surface,
        shape = RoundedCornerShape(14.dp), shadowElevation = 4.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (isFolder) "폴더 변경 확인" else "사진 이동·삭제 확인",
                style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            GalleryPathPreviewCard("현재 경로", oldPath)
            GalleryPathChangeArrow()
            GalleryPathPreviewCard(
                if (step.endsWith("delete")) "삭제 대상 경로" else "변경 후 경로",
                newPath,
            )
            Text("안내 화면에서 실제 파일은 변경되지 않습니다.",
                color = DDZColor.TextSecondary, style = DDZTypography.Caption)
        }
    }
}
