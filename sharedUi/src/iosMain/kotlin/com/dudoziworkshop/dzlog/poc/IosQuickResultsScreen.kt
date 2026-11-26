package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.feature.log.policy.CapturePathImpact
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
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
    if (album) {
        IosPhotoKitGalleryScreen(repository, onBack = { album = false }, capturePathImpact = capturePathImpact)
        return
    }
    opened?.let { initial ->
        IosPhotoKitGalleryScreen(repository, onBack = { opened = null },
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
            Text(if (projectTitle == null) "빠른결과" else "프로젝트 결과",
                Modifier.weight(1f), style = DDZTypography.ScreenTitle)
            TextButton(onClick = { album = true }) { Text("앨범 보기") }
        }
        if (scopeError != null) Text(scopeError)
        when {
            captureIds.isNotEmpty() && !permissions.photos.readable -> PermissionCard(camera = false, access = permissions.photos)
            loading -> CircularProgressIndicator()
            error != null -> {
                Text(error.orEmpty())
                TextButton(onClick = { revision++ }) { Text("다시 시도") }
            }
            captures.isEmpty() -> Text("확인할 수 있는 촬영 사진이 없습니다.")
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${projectTitle ?: "이번 촬영"} · ${captures.size}건", Modifier.weight(1f))
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
                LazyVerticalGrid(GridCells.Fixed(gridColumns), state = grid,
                    modifier = Modifier.iosGalleryGridPinch(gridColumns, enabled = true) { columns ->
                        val normalized = SavedGalleryGridPolicy.normalize(columns)
                        gridColumns = normalized
                        galleryPreferences.setQuickResultGridColumns(normalized)
                    },
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.forEach { section ->
                        item(key = section.key, span = { GridItemSpan(maxLineSpan) }) {
                            TextButton(onClick = {
                                collapsed = if (section.key in collapsed) collapsed - section.key else collapsed + section.key
                            }) {
                                Text(QuickResultsSessionPolicy.folderLabel(section.folderPath) + " · " +
                                    QuickResultsSessionPolicy.sectionLabel(section) + if (section.key in collapsed) " ▾" else " ▴")
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
                                viewerIds = sections.filterNot { it.key in collapsed }.flatMap { visible ->
                                    visible.captures.map { captureIds[it.captureIndex] }
                                }
                                opened = photo.uri
                            }) {
                                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(DDZColor.Surface),
                                    contentAlignment = Alignment.Center) {
                                    thumbnail?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                        ?: Text(if (capture.isOriginalOnly) "원본" else "결과")
                                    IosGalleryCounterBadge(
                                        photo.displayName,
                                        photo.relativePath,
                                        Modifier.align(Alignment.BottomStart).padding(5.dp),
                                    )
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
