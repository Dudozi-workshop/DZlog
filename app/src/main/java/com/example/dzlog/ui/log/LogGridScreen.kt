@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.dzlog.ui.log

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.example.dzlog.data.mediastore.DzlogMediaStoreReader
import com.example.dzlog.domain.model.MediaImageItem

/**
 * 그룹 내부 그리드 화면
 * - 탭: 뷰어로 이동
 * - 길게누르기: 다중 선택 모드 진입
 */
@Composable
fun LogGridScreen(
    g1: String,
    g2: String,
    items: List<MediaImageItem>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onItemsLoaded: (List<MediaImageItem>) -> Unit,
    onBack: () -> Unit,
    onOpenViewer: (startIndex: Int) -> Unit,
    onToggleSelection: (id: Long) -> Unit,
    onEnterSelectionWith: (id: Long) -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val reader = remember { DzlogMediaStoreReader(resolver) }

    var error by remember { mutableStateOf<String?>(null) }
    val relativePath = remember(g1, g2) { buildRelativePathFromG1G2(g1, g2) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDeleteUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var pendingDeleteCount by remember { mutableStateOf(0) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        // 시스템 삭제 요청 결과에 상관없이 목록 재조회
        runCatching { reader.loadImages(relativePath) }
            .onSuccess {
                onItemsLoaded(it)
                error = null
            }
            .onFailure { e ->
                error = e.message ?: "불러오기 실패"
            }
        onExitSelection()
    }

    fun startDeleteRequest(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi: PendingIntent = MediaStore.createDeleteRequest(resolver, uris)
            val req = IntentSenderRequest.Builder(pi.intentSender).build()
            deleteLauncher.launch(req)
            return
        }

        // Android 10 이하: 직접 delete 시도 (권한/환경에 따라 실패 가능)
        uris.forEach { uri ->
            runCatching { resolver.delete(uri, null, null) }
        }
        // 재조회
        runCatching { reader.loadImages(relativePath) }
            .onSuccess { onItemsLoaded(it) }
        onExitSelection()
    }

    LaunchedEffect(relativePath) {
        // 이미 로딩된 상태면 재조회하지 않음(뷰어->그리드 왕복 시 깜빡임 방지)
        if (items.isNotEmpty()) return@LaunchedEffect
        runCatching { reader.loadImages(relativePath) }
            .onSuccess {
                onItemsLoaded(it)
                error = null
            }
            .onFailure { e ->
                error = e.message ?: "불러오기 실패"
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Top bar
        if (isSelectionMode) {
            SelectionTopBar(
                selectedCount = selectedIds.size,
                onClose = onExitSelection,
                onSelectAll = onSelectAll
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("INSIDE GROUP")
                    Text("$g1 / $g2")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onBack) { Text("Back") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (error != null) {
            Text("오류: $error")
            return@Column
        }

        if (items.isEmpty()) {
            Text("사진이 없습니다.")
            Text("(결과물만 표시되며 original/ 폴더는 제외됩니다.)")
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
                val selected = selectedIds.contains(item.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(item.id)
                                } else {
                                    onOpenViewer(idx)
                                }
                            },
                            onLongClick = {
                                onEnterSelectionWith(item.id)
                            }
                        )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DzThumbnail(uriString = item.uri.toString())

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

                        // 날짜/텍스트는 MVP에서는 생략(성능/가독성). 필요하면 여기에서 오버레이 추가.
                    }
                }
            }
        }
    }

    if (isSelectionMode) {
        SelectionBottomBar(
            onShare = {
                val selectedItems = items.filter { selectedIds.contains(it.id) }
                shareImages(context, selectedItems)
            },
            shareEnabled = selectedIds.isNotEmpty(),
            onDelete = if (selectedIds.isNotEmpty()) {
                {
                    val selectedItems = items.filter { selectedIds.contains(it.id) }
                    pendingDeleteCount = selectedItems.size
                    pendingDeleteUris = selectedItems.map { it.uri }
                    showDeleteConfirm = true
                }
            } else null
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("삭제 확인") },
            text = { Text("선택한 사진 $pendingDeleteCount 장을 삭제합니다. 계속할까요?") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    startDeleteRequest(pendingDeleteUris)
                }) { Text("삭제") }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false }) { Text("취소") }
            }
        )
    }
}

private fun buildRelativePathFromG1G2(g1: String, g2: String): String {
    val DEFAULT = "(기본)"
    return when {
        g1 == DEFAULT -> "Pictures/DZlog/"
        g2 == DEFAULT -> "Pictures/DZlog/$g1/"
        else -> "Pictures/DZlog/$g1/$g2/"
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
