@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.dudoziworkshop.dzlog.ui.log

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
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
import com.dudoziworkshop.dzlog.feature.log.policy.launchMediaDeleteRequest
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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
    val reader = remember { DzlogMediaStoreReader(resolver) }
    val favoritesRepository = remember(context) { FavoritesProvider.repo(context) }
    val favoriteIds by favoritesRepository.favoriteIdsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val filmstripListState = rememberLazyListState()

    fun reloadAfterDelete() {
        runCatching { reader.loadImages(relativePath) }
            .onSuccess { reloaded ->
                onItemsReloaded(reloaded)
                if (reloaded.isEmpty()) {
                    onRequestCloseViewer()
                }
            }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        reloadAfterDelete()
    }

    fun startDeleteRequest(uris: List<android.net.Uri>) {
        launchMediaDeleteRequest(
            resolver = resolver,
            uris = uris,
            onLaunchIntentSender = deleteLauncher::launch,
            onLegacyDeleteCompleted = ::reloadAfterDelete
        )
    }

    val safeStart = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeStart, pageCount = { items.size })

    var uiVisible by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var isCurrentImageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(safeStart) {
        if (items.isNotEmpty()) {
            pagerState.scrollToPage(safeStart)
        }
    }

    LaunchedEffect(items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        val max = (items.size - 1).coerceAtLeast(0)
        val clamped = pagerState.currentPage.coerceIn(0, max)
        if (clamped != pagerState.currentPage) {
            pagerState.scrollToPage(clamped)
        }
    }

    val currentItem = items.getOrNull(pagerState.currentPage)

    LaunchedEffect(pagerState.currentPage, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        if (pagerState.currentPage !in items.indices) return@LaunchedEffect
        // 정책: 현재 페이지가 바뀌면 새 이미지는 1x 초기 상태이므로 pager 잠금 상태를 해제한다.
        isCurrentImageZoomed = false
        filmstripListState.animateScrollToItem(pagerState.currentPage)
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
                userScrollEnabled = !isCurrentImageZoomed
            ) { page ->
                val item = items[page]
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DzFullImage(
                        uriString = item.uri.toString(),
                        canGoPrevious = page > 0,
                        canGoNext = page < items.lastIndex,
                        onGoPrevious = {
                            if (page > 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(page - 1)
                                }
                            }
                        },
                        onGoNext = {
                            if (page < items.lastIndex) {
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

        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ViewerTopOverlay(
                current = currentItem,
                onBack = onBack,
                onShare = {
                    val item = currentItem ?: return@ViewerTopOverlay
                    shareImages(context, listOf(item))
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
                        onFavorite = {
                            val item = currentItem ?: return@ViewerBottomPill
                            scope.launch {
                                favoritesRepository.toggleFavorite(item)
                            }
                        },
                        isFavorite = currentItem?.id?.let(favoriteIds::contains) == true,
                        onInfo = {
                            if (currentItem != null) showInfoSheet = true
                        },
                        onDelete = {
                            val item = currentItem ?: return@ViewerBottomPill
                            startDeleteRequest(listOf(item.uri))
                        }
                    )

                    ThumbnailFilmstrip(
                        items = items,
                        currentPage = pagerState.currentPage,
                        favoriteIds = favoriteIds,
                        listState = filmstripListState,
                        onThumbnailClick = { index ->
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                                filmstripListState.animateScrollToItem(index)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showInfoSheet && currentItem != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState,
        ) {
            InfoSheetContent(item = currentItem)
        }
    }
}

@Composable
private fun ViewerTopOverlay(
    current: MediaImageItem?,
    onBack: () -> Unit,
    onShare: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x99000000), Color.Transparent)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                )
                .padding(start = 12.dp, end = 76.dp, top = 6.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "DZLOG VIEWER",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            IconButton(
                onClick = onShare,
                modifier = Modifier.align(Alignment.CenterEnd),
                enabled = current != null
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ViewerBottomPill(
    onFavorite: () -> Unit,
    isFavorite: Boolean,
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
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color(0xFFFF5C7A) else Color.White
            )
        }
        IconButton(onClick = onInfo) {
            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
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
        val sidePadding = ((maxWidth - thumbSize) / 2).coerceAtLeast(0.dp)

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
