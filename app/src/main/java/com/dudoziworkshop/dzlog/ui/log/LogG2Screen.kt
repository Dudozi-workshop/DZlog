package com.dudoziworkshop.dzlog.ui.log

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.LogGroupSummary
import com.dudoziworkshop.dzlog.domain.naming.buildGalleryRelativePath
import com.dudoziworkshop.dzlog.feature.log.policy.launchMediaDeleteRequest

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
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedG2 by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun resetSelection() {
        isSelectionMode = false
        selectedG2 = emptySet()
    }

    fun reload() {
        isLoading = true
        runCatching { reader.loadG2Summaries(g1) }
            .onSuccess {
                g2Summaries = it
                error = null
                isLoading = false
            }
            .onFailure { e ->
                error = e.message ?: "불러오기 실패"
                isLoading = false
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
        launchMediaDeleteRequest(
            resolver = resolver,
            uris = uris,
            onLaunchIntentSender = deleteLauncher::launch,
            onLegacyDeleteCompleted = {
                reload()
                resetSelection()
            }
        )
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
            }) { Text(stringResource(R.string.action_back)) }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            CircularProgressIndicator()
            return@Column
        }

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
                selectedCount = selectedG2.size
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
                onClose = { resetSelection() },
                onSelectAll = {
                    selectedG2 = g2Summaries.map { it.name }.toSet()
                },
                onShare = { /* G2 단위 공유는 MVP에서는 미사용 */ },
                shareEnabled = false,
                onDelete = if (selectedG2.isNotEmpty()) {
                    {
                        val allUris = mutableListOf<android.net.Uri>()
                        selectedG2.forEach { g2 ->
                            val rel = buildGalleryRelativePath(g1, if (g2 == "(기본)") "" else g2)
                            val imgs = reader.loadImages(rel)
                            allUris.addAll(imgs.map { it.uri })
                        }
                        startDeleteRequest(allUris)
                    }
                } else null
            )
        }
    }
}
