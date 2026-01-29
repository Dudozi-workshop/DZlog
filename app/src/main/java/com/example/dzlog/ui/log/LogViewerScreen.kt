@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.log

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.MediaImageItem

/**
 * 사진 뷰어
 * - 좌/우 스와이프: 같은 그룹 내 이동
 * - 탭: UI 숨김/표시
 * - 길게누르기: 선택 모드 진입(현재 사진 선택)
 */
@Composable
fun LogViewerScreen(
    g1: String,
    g2: String,
    items: List<MediaImageItem>,
    startIndex: Int,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onBack: () -> Unit,
    onEnterSelectionWith: (id: Long) -> Unit,
    onToggleSelection: (id: Long) -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
) {
    val context = LocalContext.current
    val safeStart = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeStart, pageCount = { items.size })

    var uiVisible by remember { mutableStateOf(true) }

    // startIndex가 바뀌면 뷰어 시작 위치 이동(같은 세션에서 재진입 고려)
    LaunchedEffect(safeStart) {
        if (items.isNotEmpty()) {
            pagerState.scrollToPage(safeStart)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (items.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = items[page]
                val selected = selectedIds.contains(item.id)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = {
                                // 선택 모드에서는 탭=선택/해제, 일반 모드에서는 UI 토글
                                if (isSelectionMode) {
                                    onToggleSelection(item.id)
                                } else {
                                    uiVisible = !uiVisible
                                }
                            },
                            onLongClick = {
                                // 길게 누르면 선택 모드 진입 + 현재 사진 선택
                                onEnterSelectionWith(item.id)
                                uiVisible = true
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    DzFullImage(uriString = item.uri.toString())

                    if (isSelectionMode && selected) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0x3300FF00)))
                    }
                }
            }
        }

        if (uiVisible) {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    onClose = onExitSelection,
                    onSelectAll = onSelectAll
                )
            } else {
                ViewerTopBar(
                    g1 = g1,
                    g2 = g2,
                    current = if (items.isEmpty()) 0 else (pagerState.currentPage + 1),
                    total = items.size,
                    onBack = onBack
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                ViewerBottomBar(
                    enabled = if (isSelectionMode) selectedIds.isNotEmpty() else items.isNotEmpty(),
                    onShare = {
                        val toShare = if (isSelectionMode) {
                            items.filter { selectedIds.contains(it.id) }
                        } else {
                            listOf(items.getOrNull(pagerState.currentPage) ?: return@ViewerBottomBar)
                        }
                        shareImages(context, toShare)
                    }
                )
            }
        }
    }
}

@Composable
private fun ViewerTopBar(
    g1: String,
    g2: String,
    current: Int,
    total: Int,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x66000000))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Text("$current / $total", color = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text("DZlog / $g1 / $g2", color = Color.White)
    }
}

@Composable
private fun ViewerBottomBar(
    enabled: Boolean,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x66000000))
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(onClick = onShare, enabled = enabled) { Text("Share") }
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
