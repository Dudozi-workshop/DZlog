@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@file:Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package com.dudoziworkshop.dzlog.ui.log

import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.favorites.FavoritesProvider
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.feature.log.policy.launchMediaDeleteRequest
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import com.dudoziworkshop.dzlog.ui.theme.dzTopInset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class GridSortOption { DATE, FAVORITE }

/**
 * 그룹 내부 그리드 화면
 * - 탭: 뷰어로 이동
 * - 길게누르기: 다중 선택 모드 진입
 */
@Composable
fun LogGridScreen(
    headerTitle: String,
    isOriginalGrid: Boolean,
    relativePath: String,
    originalRelativePath: String?,
    onOpenOriginalFolder: (String) -> Unit,
    items: List<MediaImageItem>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onItemsLoaded: (List<MediaImageItem>) -> Unit,
    onOpenViewer: (startIndex: Int) -> Unit,
    onToggleSelection: (id: Long) -> Unit,
    onEnterSelectionWith: (id: Long) -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val reader = remember { DzlogMediaStoreReader(resolver) }
    val favoritesRepository = remember(context) { FavoritesProvider.repo(context) }
    val favoriteIds by favoritesRepository.favoriteIdsFlow.collectAsState(initial = emptySet())

    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var originalCount by remember { mutableIntStateOf(0) }
    var originalLatestUri by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    var reloadJob by remember { mutableStateOf<Job?>(null) }
    var sortOption by rememberSaveable { mutableStateOf(GridSortOption.DATE) }

    val displayItems = remember(items, favoriteIds, sortOption) {
        when (sortOption) {
            GridSortOption.DATE -> items
            GridSortOption.FAVORITE -> items.sortedWith(
                compareByDescending<MediaImageItem> { favoriteIds.contains(it.id) }
                    .thenByDescending { it.dateAddedSeconds }
            )
        }
    }

    val indexById = remember(items) {
        items.mapIndexed { index, item -> item.id to index }.toMap()
    }

    fun reloadImages() {
        // ✅ MediaStore 변경 이벤트가 연속으로 들어올 수 있어 디바운스 처리
        reloadJob?.cancel()
        reloadJob = scope.launch {
            isLoading = true
            try {
                // 스캔/메타 변경 이벤트가 연속으로 들어올 때 재조회 폭주 체감 줄이기
                delay(500)
                val loaded = withContext(Dispatchers.IO) {
                    reader.loadImages(relativePath)
                }
                onItemsLoaded(loaded)
                error = null
            } catch (t: Throwable) {
                error = t.message ?: "불러오기 실패"
            } finally {
                isLoading = false
            }
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        // 시스템 삭제 요청 결과에 상관없이 목록 재조회
        reloadImages()
        onExitSelection()
    }

    fun startDeleteRequest(uris: List<android.net.Uri>) {
        launchMediaDeleteRequest(
            resolver = resolver,
            uris = uris,
            onLaunchIntentSender = deleteLauncher::launch,
            onLegacyDeleteCompleted = {
                reloadImages()
                onExitSelection()
            }
        )
    }

    fun selectedItems(): List<MediaImageItem> =
        items.filter { selectedIds.contains(it.id) }

    LaunchedEffect(relativePath) {
        reloadImages()
    }



    suspend fun reloadOriginalCard() {
        if (originalRelativePath.isNullOrBlank()) {
            originalCount = 0
            originalLatestUri = null
        } else {
            val (count, latestUri) = withContext(Dispatchers.IO) {
                val count = reader.countImagesInRelativePath(originalRelativePath)
                val latest = reader.loadLatestImageInRelativePath(originalRelativePath)?.uri?.toString()
                count to latest
            }
            originalCount = count
            originalLatestUri = latestUri
        }
    }

    LaunchedEffect(originalRelativePath) {
        reloadOriginalCard()
    }

    // ✅ 앱 밖 변경(휴지통 복구/삭제 등)을 앱이 즉시 반영하도록 MediaStore 변경 감지
    // 선택 중에는 reload를 막아 버벅임 감소 (선택 해제 후 필요 시 수동/다른 트리거로 갱신)
    DisposableEffect(relativePath, isSelectionMode) {
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                // 선택 모드 중엔 자동 재조회로 UI가 흔들리고 버벅임이 심해져서 차단
                if (isSelectionMode) return
                reloadImages()
                scope.launch {
                    reloadOriginalCard()
                }
            }
        }

        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    // 하단 액션바가 있을 때 그리드 마지막 줄이 가려지지 않도록 여백
    val bottomInset = if (isSelectionMode) 84.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Top bar
            if (isSelectionMode) {
                // ✅ 상단은 카운트만 (뒤로가기/타이틀 겹침 방지)
                Box(modifier = Modifier.fillMaxWidth().padding(top = dzTopInset())) {
                    SelectionTopBar(
                        selectedCount = selectedIds.size
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dzTopInset()),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterStart),
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            tint = DDZColor.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = headerTitle,
                        modifier = Modifier.padding(horizontal = 56.dp),
                        textAlign = TextAlign.Center,
                        style = DDZTypography.ScreenTitle,
                        color = DDZColor.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortToggleChip(
                            text = "날짜",
                            selected = sortOption == GridSortOption.DATE,
                            onClick = { sortOption = GridSortOption.DATE }
                        )
                        SortToggleChip(
                            text = "좋아요",
                            selected = sortOption == GridSortOption.FAVORITE,
                            onClick = { sortOption = GridSortOption.FAVORITE }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (!isOriginalGrid && !originalRelativePath.isNullOrBlank() && originalCount > 0) {
                    LogOriginalPhotoEntryCard(
                        count = originalCount,
                        latestUriString = originalLatestUri,
                        onClick = { onOpenOriginalFolder(originalRelativePath) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (error != null) {
                Text("오류: $error")
                return@Column
            }

            // ✅ 최초/재진입 시 empty 먼저 그려지는 깜빡임 방지
            if (isLoading && items.isEmpty()) {
                CircularProgressIndicator()
                return@Column
            }

            if (items.isEmpty()) {
                Text("사진이 없습니다.")
                Text("(선택한 폴더의 사진만 표시됩니다.)")
                return@Column
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(bottom = bottomInset)
            ) {
                itemsIndexed(displayItems, key = { _, it -> it.id }) { _, item ->
                    val selected = selectedIds.contains(item.id)
                    val isFavorite = favoriteIds.contains(item.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        onToggleSelection(item.id)
                                    } else {
                                        val originalIndex = indexById[item.id] ?: 0
                                        onOpenViewer(originalIndex)
                                    }
                                },
                                onLongClick = { onEnterSelectionWith(item.id) }
                            )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            DzThumbnail(uriString = item.uri.toString())

                            if (isFavorite) {
                                Box(
                                    modifier = Modifier
                                        .padding(6.dp)
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

                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x66000000))
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ✅ 선택 액션은 하단 고정
        if (isSelectionMode) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                SelectionBottomBar(
                    onClose = onExitSelection,
                    onSelectAll = onSelectAll,
                    onShare = {
                        shareImages(context, selectedItems())
                    },
                    shareEnabled = selectedIds.isNotEmpty(),
                    onDelete = if (selectedIds.isNotEmpty()) {
                        {
                            startDeleteRequest(selectedItems().map { it.uri })
                        }
                    } else null
                )
            }
        }
    }
    // SelectionBottomBar는 Box 하단 고정으로 이동됨
}

@Composable
private fun SortToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) Color(0xFF2D2D2D) else Color(0xFF1B1B1B),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.LightGray,
        )
    }
}

private fun shareImages(context: android.content.Context, items: List<MediaImageItem>) {

    if (items.isEmpty()) return
    val uris = ArrayList(items.map { it.uri })
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "공유"))
}
