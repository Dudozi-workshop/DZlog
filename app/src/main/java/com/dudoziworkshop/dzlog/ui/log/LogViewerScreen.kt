@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.dudoziworkshop.dzlog.ui.log

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.favorites.FavoritesProvider
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryViewerDeletePolicy
import com.dudoziworkshop.dzlog.data.mediastore.GallerySnapshotMemory
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryPhotoMovePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

@Composable
fun LogViewerScreen(
    relativePath: String,
    items: List<MediaImageItem>,
    startIndex: Int,
    onBack: () -> Unit,
    onItemsReloaded: (List<MediaImageItem>) -> Unit,
    onRequestCloseViewer: () -> Unit,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val reader = remember(resolver) { DzlogMediaStoreReader(resolver) }
    val favoritesRepository = remember(context) { FavoritesProvider.repo(context) }
    val favoriteIds by favoritesRepository.favoriteIdsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val filmstripListState = rememberLazyListState()

    var originalPreview by remember { mutableStateOf<MediaImageItem?>(null) }
    var originalLoading by remember { mutableStateOf(false) }
    var originalError by remember { mutableStateOf<String?>(null) }
    var deleteItem by remember { mutableStateOf<MediaImageItem?>(null) }
    var pendingViewerIndex by remember { mutableStateOf<Int?>(null) }
    var emptyAfterDelete by remember { mutableStateOf(false) }
    val operationLocked = deleteItem != null || originalLoading
    BackHandler(enabled = originalPreview != null && !operationLocked) { originalPreview = null }
    BackHandler(enabled = operationLocked) { /* The deletion dialog owns dismissal. */ }

    val safeStart = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeStart, pageCount = { items.size })

    var uiVisible by remember { mutableStateOf(true) }
    var filmstripExpanded by remember { mutableStateOf(false) }
    var infoSheetItem by remember { mutableStateOf<MediaImageItem?>(null) }
    var isCurrentImageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            val target = (pendingViewerIndex ?: pagerState.currentPage).coerceIn(items.indices)
            pagerState.scrollToPage(target)
        }
        pendingViewerIndex = null
    }

    val resultItem = items.getOrNull(pagerState.currentPage)
    val currentItem = originalPreview ?: resultItem
    LaunchedEffect(resultItem?.id) { originalPreview = null }

    fun showOriginal() {
        if (operationLocked) return
        if (originalPreview != null) {
            originalPreview = null
            return
        }
        val requested = resultItem ?: return
        originalLoading = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val snapshot = reader.loadImagesUnderPrefix(GalleryFolderIndexPolicy.ROOT)
                    require(snapshot.singleOrNull { it.id == requested.id } == requested) {
                        "사진 정보가 변경됐습니다. 갤러리에서 다시 확인해 주세요."
                    }
                    requireNotNull(GalleryPhotoMovePolicy.pairedOriginals(listOf(requested), snapshot).singleOrNull()) {
                        "정확히 연결된 원본사진을 찾지 못했습니다."
                    }
                }
            }
            originalLoading = false
            result.onSuccess {
                if (items.getOrNull(pagerState.currentPage)?.id == requested.id) originalPreview = it
                else originalError = "현재 사진이 바뀌었습니다. 다시 원본 보기를 선택해 주세요."
            }
                .onFailure { originalError = it.message ?: "원본사진을 확인하지 못했습니다." }
        }
    }

    LaunchedEffect(pagerState.currentPage, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        if (pagerState.currentPage !in items.indices) return@LaunchedEffect
        // 정책: 현재 페이지가 바뀌면 새 이미지는 1x 초기 상태이므로 pager 잠금 상태를 해제한다.
        isCurrentImageZoomed = false
    }

    LaunchedEffect(filmstripExpanded, uiVisible, pagerState.currentPage, items.size) {
        if (filmstripExpanded && uiVisible && pagerState.currentPage in items.indices) {
            filmstripListState.scrollToItem(pagerState.currentPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (items.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // 정책: 확대 상태에서는 부모 pager 스크롤을 명시적으로 잠근다.
                userScrollEnabled = !isCurrentImageZoomed && !operationLocked && originalPreview == null
            ) { page ->
                val item = items[page]
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DzFullImage(
                        uriString = item.uri.toString(),
                        canGoPrevious = page > 0 && !operationLocked,
                        canGoNext = page < items.lastIndex && !operationLocked,
                        onGoPrevious = {
                            if (page > 0 && !operationLocked) {
                                scope.launch {
                                    pagerState.animateScrollToPage(page - 1)
                                }
                            }
                        },
                        onGoNext = {
                            if (page < items.lastIndex && !operationLocked) {
                                scope.launch {
                                    pagerState.animateScrollToPage(page + 1)
                                }
                            }
                        },
                        onZoomedStateChange = { zoomed ->
                            // 현재 페이지의 확대 상태만 pager 잠금 조건으로 사용한다.
                            if (page == pagerState.currentPage) {
                                isCurrentImageZoomed = zoomed
                            }
                        },
                        onSingleTap = { uiVisible = !uiVisible },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        originalPreview?.let { original ->
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                DzFullImage(
                    uriString = original.uri.toString(),
                    canGoPrevious = false,
                    canGoNext = false,
                    onGoPrevious = {},
                    onGoNext = {},
                    onZoomedStateChange = {},
                    onSingleTap = { uiVisible = !uiVisible },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ViewerTopOverlay(
                current = currentItem,
                onBack = { if (!operationLocked) { if (originalPreview != null) originalPreview = null else onBack() } },
                enabled = !operationLocked,
                currentIndex = pagerState.currentPage,
                total = items.size,
                showingOriginal = originalPreview != null || currentItem?.let { GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) } == true,
                originalActionLabel = if (originalPreview != null) "결과사진으로 돌아가기" else "원본 보기",
                onOriginal = if (originalPreview != null || resultItem?.let { !GalleryPhotoMovePolicy.isOriginalPath(it.relativePath) } == true) ::showOriginal else null,
                onInfo = {
                    if (!operationLocked && currentItem != null) infoSheetItem = currentItem
                }
            )
        }

        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xAA000000))
                        )
                    )
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ViewerBottomPill(
                        enabled = !operationLocked && currentItem != null,
                        onFavorite = {
                            if (operationLocked) return@ViewerBottomPill
                            val item = currentItem ?: return@ViewerBottomPill
                            scope.launch {
                                favoritesRepository.toggleFavorite(item)
                            }
                        },
                        isFavorite = currentItem?.id?.let(favoriteIds::contains) == true,
                        onShare = {
                            if (operationLocked) return@ViewerBottomPill
                            val item = currentItem ?: return@ViewerBottomPill
                            shareImages(context, listOf(item))
                        },
                        onInfo = {
                            if (!operationLocked && currentItem != null) infoSheetItem = currentItem
                        },
                        onDelete = {
                            if (!operationLocked && currentItem != null) {
                                emptyAfterDelete = false
                                deleteItem = currentItem
                            }
                        }
                    )

                    TextButton(
                        onClick = { filmstripExpanded = !filmstripExpanded },
                        enabled = !operationLocked && originalPreview == null && items.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = if (filmstripExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.White,
                        )
                        Text(
                            text = if (filmstripExpanded) "사진 목록 접기" else "사진 목록 펼치기",
                            color = Color.White,
                        )
                    }
                    if (filmstripExpanded && originalPreview == null) {
                        ThumbnailFilmstrip(
                            items = items,
                            currentPage = pagerState.currentPage,
                            favoriteIds = favoriteIds,
                            listState = filmstripListState,
                            onThumbnailClick = { index ->
                                if (!operationLocked && originalPreview == null && index in items.indices) scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    deleteItem?.let { requested ->
        GalleryViewerDeleteDialog(
            item = requested,
            reader = reader,
            onVerified = { deletedIds ->
                if (originalPreview?.id in deletedIds) originalPreview = null
                GallerySnapshotMemory.cache.invalidate()
                if (items.any { it.id in deletedIds }) {
                    val update = GalleryViewerDeletePolicy.reconcile(items.map { it.id }, pagerState.currentPage, deletedIds)
                    pendingViewerIndex = update.currentIndex
                    emptyAfterDelete = update.ids.isEmpty()
                    onItemsReloaded(items.filter { it.id !in deletedIds })
                }
            },
            onClose = {
                deleteItem = null
                if (emptyAfterDelete) onRequestCloseViewer()
            },
        )
    }

    if (originalLoading || originalError != null) {
        AlertDialog(
            onDismissRequest = { if (!originalLoading) originalError = null },
            title = { Text(if (originalLoading) "원본 확인 중" else "원본 보기") },
            text = { Text(originalError ?: "연결된 원본사진을 확인하고 있습니다.") },
            confirmButton = {
                if (!originalLoading) TextButton(onClick = { originalError = null }) { Text("확인") }
            },
        )
    }

    val sheetItem = infoSheetItem
    if (sheetItem != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { infoSheetItem = null },
            sheetState = sheetState,
        ) {
            InfoSheetContent(item = sheetItem)
        }
    }
}

@Composable
private fun ViewerTopOverlay(
    current: MediaImageItem?,
    enabled: Boolean,
    onBack: () -> Unit,
    currentIndex: Int,
    total: Int,
    showingOriginal: Boolean,
    originalActionLabel: String,
    onOriginal: (() -> Unit)?,
    onInfo: () -> Unit,
) {
    var menuExpanded by remember(current?.id, enabled) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent)))
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, enabled = enabled) {
            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = Color.White)
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = current?.displayName.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${if (showingOriginal) "원본 · " else ""}${if (current == null) 0 else currentIndex + 1} / $total",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, enabled = enabled && current != null) {
                Icon(Icons.Default.MoreVert, contentDescription = "사진 메뉴", tint = Color.White)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (onOriginal != null) DropdownMenuItem(
                    text = { Text(originalActionLabel) },
                    onClick = {
                        menuExpanded = false
                        if (enabled) onOriginal()
                    },
                )
                DropdownMenuItem(
                    text = { Text("사진 정보") },
                    onClick = {
                        menuExpanded = false
                        if (enabled && current != null) onInfo()
                    },
                )
            }
        }
    }
}

@Composable
private fun ViewerBottomPill(
    enabled: Boolean,
    onFavorite: () -> Unit,
    isFavorite: Boolean,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            // 정책 변경: 전체 하단 바 대신 버튼 그룹만 pill 배경으로 강조한다.
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 18.dp, vertical = DDZLayout.Spacing.XS),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onFavorite, enabled = enabled) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가",
                tint = if (isFavorite) Color(0xFFFF5C7A) else Color.White
            )
        }
        IconButton(onClick = onShare, enabled = enabled) {
            Icon(Icons.Default.Share, contentDescription = "공유", tint = Color.White)
        }
        IconButton(onClick = onInfo, enabled = enabled) {
            Icon(Icons.Default.Info, contentDescription = "사진 정보", tint = Color.White)
        }
        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(Icons.Default.Delete, contentDescription = "사진 삭제", tint = Color.White)
        }
    }
}

@Composable
private fun ThumbnailFilmstrip(
    items: List<MediaImageItem>,
    currentPage: Int,
    favoriteIds: Set<Long>,
    listState: LazyListState,
    onThumbnailClick: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val thumbSize = 60.dp
        val sidePadding = ((this@BoxWithConstraints.maxWidth - thumbSize) / 2).coerceAtLeast(0.dp)

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = sidePadding),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val selected = index == currentPage
                val isFavorite = favoriteIds.contains(item.id)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(thumbSize)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Color.White else Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onThumbnailClick(index) },
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DzThumbnail(uriString = item.uri.toString())

                        if (isFavorite) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color(0xCC000000), shape = CircleShape)
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5C7A),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSheetContent(item: MediaImageItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "사진 정보",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider()
        InfoRow(label = "저장경로", value = item.relativePath)
        InfoRow(label = "파일명", value = item.displayName)
        InfoRow(label = "촬영일시", value = formatDateTime(item.dateAddedSeconds))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun shareImages(context: android.content.Context, items: List<MediaImageItem>) {
    if (items.isEmpty()) return
    val uris = ArrayList(items.map { it.uri })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, "공유"))
}

private fun formatDateTime(dateAddedSeconds: Long): String {
    val locale = Locale.getDefault()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val zonedDateTime = Instant.ofEpochSecond(dateAddedSeconds).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(zonedDateTime)
    } else {
        java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.MEDIUM,
            java.text.DateFormat.MEDIUM,
            locale
        ).format(Date(dateAddedSeconds * 1000))
    }
}
