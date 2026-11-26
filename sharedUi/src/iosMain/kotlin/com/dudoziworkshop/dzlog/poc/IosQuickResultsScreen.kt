package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.feature.log.policy.CapturePathImpact
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.*
import com.dudoziworkshop.dzlog.domain.quickresults.*
import com.dudoziworkshop.dzlog.ui.common.AlbumViewAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.CancellationException
import org.jetbrains.skia.Image as SkiaImage

/** IDs are in shutter order, oldest first, including gaps left by missing Photos assets. */
internal fun quickResultCaptures(ids: List<String>, photos: List<SavedCaptureMedia>): List<QuickResultCapture> {
    val allowed = ids.toHashSet()
    val present = photos.filter { it.captureId in allowed && it.availability == CaptureMediaAvailability.PRESENT }
    val byCapture = present.groupBy({ it.captureId }, { it.asset.assetId })
    return QuickResultsSessionPolicy.captures(ids.map { byCapture[it].orEmpty() }, present.map {
        val target = it.asset.target
        QuickResultPhoto(it.asset.assetId, target.displayName, target.relativePath, target.role)
    })
}

@Composable
internal fun IosQuickResultsScreen(
    repository: PhotoKitCaptureMediaRepository,
    captureIds: List<String>,
    projectTitle: String?,
    onBack: () -> Unit,
    scopeError: String? = null,
    capturePathImpact: (String) -> CapturePathImpact,
) {
    var album by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<String?>(null) }
    var viewerIds by remember { mutableStateOf(emptyList<String>()) }
    var folder by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(emptySet<String>()) }
    var revision by remember { mutableIntStateOf(0) }
    val grid = rememberLazyGridState()
    val galleryPreferences = remember { IosGalleryPreferences() }
    var gridColumns by remember { mutableStateOf(galleryPreferences.quickResultGridColumns()) }
    var pendingGridAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(gridColumns, pendingGridAnchor) {
        pendingGridAnchor?.let { (index, offset) ->
            grid.scrollToItem(index, offset)
            pendingGridAnchor = null
        }
    }
    if (album) {
        IosPhotoKitGalleryScreen(repository, onBack = {
            album = false
            revision++
        }, capturePathImpact = capturePathImpact)
        return
    }
    opened?.let { initial ->
        IosPhotoKitGalleryScreen(repository, onBack = {
            opened = null
            revision++
        },
            resultCaptureIds = viewerIds, initialAssetId = initial, capturePathImpact = capturePathImpact)
        return
    }
    val permissions = rememberIosPermissions()
    var captures by remember(permissions.photoScopeVersion) { mutableStateOf(emptyList<QuickResultCapture>()) }
    var loading by remember(permissions.photoScopeVersion) { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(repository, captureIds, revision, permissions.photoScopeVersion) {
        loading = true
        error = null
        try {
            captures = if (captureIds.isEmpty() || !permissions.photos.readable) emptyList()
                else quickResultCaptures(captureIds, repository.listSavedAssets())
            if (folder !in QuickResultsSessionPolicy.folderChoices(captures)) folder = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "사진을 불러오지 못했습니다."
        } finally { loading = false }
    }
    val sections = remember(captures, folder) { QuickResultsSessionPolicy.sections(captures, folder) }
    Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("뒤로") }
            Column(Modifier.weight(1f)) {
                Text(
                    if (projectTitle == null) "빠른결과" else "프로젝트 결과",
                    style = DDZTypography.ScreenTitle,
                    color = DDZColor.TextPrimary,
                )
                Text(
                    "${projectTitle ?: "이번 촬영"} · ${captures.size}건",
                    color = DDZColor.TextSecondary,
                    style = DDZTypography.Secondary,
                )
            }
            AlbumViewAction(onClick = { album = true })
        }
        if (scopeError != null) {
            Surface(
                Modifier.fillMaxWidth(),
                color = DDZColor.SurfaceSoft,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    scopeError,
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = DDZColor.TextSecondary,
                    style = DDZTypography.Caption,
                )
            }
        }
        when {
            captureIds.isNotEmpty() && !permissions.photos.readable -> PermissionCard(camera = false, access = permissions.photos)
            loading -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            error != null -> {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error.orEmpty(), color = DDZColor.TextSecondary)
                        TextButton(onClick = { revision++ }) { Text("다시 시도") }
                    }
                }
            }
            captures.isEmpty() -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (projectTitle == null) "이번 촬영 사진이 없습니다."
                    else "확인할 수 있는 촬영 사진이 없습니다.",
                    color = DDZColor.TextSecondary,
                )
            }
            else -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        TextButton(onClick = { menu = true }) {
                            Text(folder?.let(QuickResultsSessionPolicy::folderLabel) ?: "전체 폴더")
                        }
                        DropdownMenu(menu, { menu = false }) {
                            (listOf(null) + QuickResultsSessionPolicy.folderChoices(captures)).forEach { path ->
                                DropdownMenuItem(text = { Text(path?.let(QuickResultsSessionPolicy::folderLabel) ?: "전체 폴더") },
                                    onClick = { folder = path; menu = false })
                            }
                        }
                    }
                }
                if (sections.isEmpty()) {
                    Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("선택한 폴더에 이번 촬영 사진이 없습니다.", color = DDZColor.TextSecondary)
                    }
                } else LazyVerticalGrid(GridCells.Fixed(gridColumns), state = grid,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .iosGalleryGridPinch(gridColumns, enabled = true) { columns ->
                        val normalized = SavedGalleryGridPolicy.normalize(columns)
                        if (normalized != gridColumns) {
                            pendingGridAnchor = grid.firstVisibleItemIndex to grid.firstVisibleItemScrollOffset
                            gridColumns = normalized
                            galleryPreferences.setQuickResultGridColumns(normalized)
                        }
                        },
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.forEach { section ->
                        item(key = section.key, span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(DDZColor.Surface, RoundedCornerShape(14.dp))
                                    .clickable {
                                        collapsed = if (section.key in collapsed) {
                                            collapsed - section.key
                                        } else {
                                            collapsed + section.key
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    QuickResultsSessionPolicy.folderLabel(section.folderPath),
                                    color = DDZColor.TextPrimary,
                                    style = DDZTypography.SettingLabel,
                                )
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        QuickResultsSessionPolicy.sectionLabel(section) +
                                            if (section.key in collapsed) " ▾" else " ▴",
                                        color = DDZColor.TextSecondary,
                                        style = DDZTypography.Caption,
                                    )
                                }
                            }
                        }
                        if (section.key !in collapsed) items(section.captures, key = { it.displayedPhoto.uri }) { capture ->
                            val photo = capture.displayedPhoto
                            val thumbnail by produceState<ImageBitmap?>(null, photo.uri) {
                                value = try {
                                    repository.readThumbnail(photo.uri)?.let { bytes ->
                                        SkiaImage.makeFromEncoded(uprightIosImage(bytes)).toComposeImageBitmap()
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            Column(Modifier.clickable {
                                viewerIds = sections.flatMap { visible ->
                                    visible.captures.map { captureIds[it.captureIndex] }
                                }
                                opened = photo.uri
                            }) {
                                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(DDZColor.Surface),
                                    contentAlignment = Alignment.Center) {
                                    thumbnail?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                        ?: Text("사진")
                                    IosGalleryCounterBadge(
                                        photo.displayName,
                                        photo.relativePath,
                                        Modifier.align(Alignment.BottomStart).padding(5.dp),
                                    )
                                    if (capture.isOriginalOnly) {
                                        Text(
                                            "원본",
                                            modifier = Modifier.align(Alignment.TopStart)
                                                .padding(DDZLayout.QuickOriginalBadge.EdgeInset)
                                                .background(
                                                    DDZColor.QuickOriginalBadgeBackground,
                                                    RoundedCornerShape(DDZLayout.QuickOriginalBadge.Radius),
                                                )
                                                .border(
                                                    DDZLayout.QuickOriginalBadge.BorderWidth,
                                                    DDZColor.QuickOriginalBadgeBorder,
                                                    RoundedCornerShape(DDZLayout.QuickOriginalBadge.Radius),
                                                )
                                                .padding(
                                                    horizontal = DDZLayout.QuickOriginalBadge.HorizontalPadding,
                                                    vertical = DDZLayout.QuickOriginalBadge.VerticalPadding,
                                                ),
                                            color = DDZColor.QuickOriginalBadgeText,
                                            style = DDZTypography.QuickOriginalBadge,
                                        )
                                    }
                                }
                                Text(photo.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
