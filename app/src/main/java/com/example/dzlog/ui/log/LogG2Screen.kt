package com.example.dzlog.ui.log

import android.app.PendingIntent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dzlog.data.mediastore.DzlogMediaStoreReader
import com.example.dzlog.domain.model.LogGroupSummary
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import android.provider.MediaStore

/**
 * G2 화면은 G1과 UI를 구분하기 위해 "큰 타일"(2열 그리드)로 구성.
 * - 길게 누르기: 선택 모드 진입 (삭제 목적)
 * - 선택 모드에서 Delete: 선택한 G2 폴더의 결과물 사진들을 MediaStore 삭제 요청
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogG2Screen(
    g1: String,
    onBack: () -> Unit,
    onSelectG2: (String) -> Unit,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val reader = remember { DzlogMediaStoreReader(resolver) }

    var g2Summaries by remember { mutableStateOf<List<LogGroupSummary>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedG2 by remember { mutableStateOf<Set<String>>(emptySet()) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDeleteUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var pendingDeleteCount by remember { mutableStateOf(0) }

    fun resetSelection() {
        isSelectionMode = false
        selectedG2 = emptySet()
    }

    fun reload() {
        runCatching { reader.loadG2Summaries(g1) }
            .onSuccess {
                g2Summaries = it
                error = null
            }
            .onFailure { e ->
                error = e.message ?: "불러오기 실패"
            }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // 시스템 삭제 요청 결과에 상관없이 목록은 다시 조회(삭제 취소 시 동일 결과)
        reload()
        resetSelection()
    }

    fun startDeleteRequest(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return

        // Android 11+ : createDeleteRequest 권장
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi: PendingIntent = MediaStore.createDeleteRequest(resolver, uris)
            val req = IntentSenderRequest.Builder(pi.intentSender).build()
            deleteLauncher.launch(req)
            return
        }

        // Android 10 이하: 직접 delete 시도 (권한/환경에 따라 실패 가능)
        var deleted = 0
        uris.forEach { uri ->
            runCatching { resolver.delete(uri, null, null) }
                .onSuccess { cnt -> if (cnt > 0) deleted += cnt }
        }
        reload()
        resetSelection()
    }

    LaunchedEffect(g1) {
        reload()
        resetSelection()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DZlog / $g1")
            Button(onClick = {
                // G2 화면을 나가면 선택은 초기화
                resetSelection()
                onBack()
            }) { Text("Back") }
        }

        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text("오류: $error")
            return@Column
        }

        if (g2Summaries.isEmpty()) {
            Text("아직 사진이 없습니다.")
            return@Column
        }

        // 선택 모드 UI
        if (isSelectionMode) {
            SelectionTopBar(
                selectedCount = selectedG2.size,
                onClose = { resetSelection() },
                onSelectAll = {
                    selectedG2 = g2Summaries.map { it.name }.toSet()
                }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(g2Summaries, key = { it.name }) { summary ->
                val selected = selectedG2.contains(summary.name)
                LogGroupTileCard(
                    summary = summary,
                    isSelected = selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    selectedG2 = if (selected) selectedG2 - summary.name else selectedG2 + summary.name
                                } else {
                                    onSelectG2(summary.name)
                                }
                            },
                            onLongClick = {
                                isSelectionMode = true
                                selectedG2 = selectedG2 + summary.name
                            }
                        )
                )
            }
        }

        // 하단 선택 액션바
        if (isSelectionMode) {
            SelectionBottomBar(
                onShare = { /* G2 단위 공유는 MVP에서는 미사용 */ },
                shareEnabled = false,
                onDelete = if (selectedG2.isNotEmpty()) {
                    {
                        // 삭제 대상 URI 수집
                        val allUris = mutableListOf<android.net.Uri>()
                        var total = 0
                        selectedG2.forEach { g2 ->
                            val rel = buildGalleryRelativePath(g1, if (g2 == "(기본)") "" else g2)
                            val imgs = reader.loadImages(rel)
                            total += imgs.size
                            allUris.addAll(imgs.map { it.uri })
                        }
                        pendingDeleteUris = allUris
                        pendingDeleteCount = total
                        showDeleteConfirm = true
                    }
                } else null
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("삭제 확인") },
            text = { Text("선택한 G2 폴더의 결과물 사진 $pendingDeleteCount 장을 삭제합니다. 계속할까요?") },
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
