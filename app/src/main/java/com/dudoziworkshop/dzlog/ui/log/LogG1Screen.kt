package com.dudoziworkshop.dzlog.ui.log

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.LogGroupSummary
import com.dudoziworkshop.dzlog.feature.log.policy.buildDeleteTargetsForG1Selection
import com.dudoziworkshop.dzlog.feature.log.policy.collectDeleteUris
import com.dudoziworkshop.dzlog.feature.log.policy.launchMediaDeleteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogG1Screen(
    onBack: () -> Unit,
    onOpenG2: (String) -> Unit,
    onOpenGridByRuleC: (
        title: String,
        waterRel: String,
        originalRel: String,
        waterCount: Int,
        originalCount: Int,
    ) -> Unit,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val reader = remember { DzlogMediaStoreReader(resolver) }
    val scope = rememberCoroutineScope()

    var rootNode by remember { mutableStateOf<DzlogMediaStoreReader.G1Node?>(null) }
    var g1Nodes by remember { mutableStateOf<List<DzlogMediaStoreReader.G1Node>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNodes by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun resetSelection() {
        isSelectionMode = false
        selectedNodes = emptySet()
    }

    fun reload() {
        isLoading = true
        runCatching { reader.loadG1Nodes() }
            .onSuccess {
                rootNode = it.firstOrNull { node -> node.name == DzlogMediaStoreReader.ROOT_G1 }
                g1Nodes = it.filter { node -> node.name != DzlogMediaStoreReader.ROOT_G1 }
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
    ) {
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

    fun prepareDeleteRequest() {
        scope.launch {
            val targets = buildDeleteTargetsForG1Selection(
                selected = selectedNodes,
                rootSelected = selectedNodes.contains(DzlogMediaStoreReader.ROOT_G1),
            )
            val uris = withContext(Dispatchers.IO) {
                collectDeleteUris(reader, targets)
            }
            if (uris.isEmpty()) {
                Toast.makeText(context, "삭제할 사진이 없습니다", Toast.LENGTH_SHORT).show()
                return@launch
            }
            startDeleteRequest(uris)
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    BackHandler(enabled = isSelectionMode) {
        resetSelection()
    }

    val bottomInset = if (isSelectionMode) 84.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("앨범")
                Button(onClick = {
                    if (isSelectionMode) {
                        resetSelection()
                    } else {
                        onBack()
                    }
                }) { Text(stringResource(R.string.action_back)) }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                CircularProgressIndicator()
                return@Column
            }

            if (error != null) {
                Text("오류: $error")
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    error = null
                    g1Nodes = emptyList()
                }) { Text("닫기") }
                return@Column
            }

            if (g1Nodes.isEmpty() && rootNode == null) {
                Text("저장된 DZlog 결과물이 없습니다.")
                Spacer(Modifier.height(6.dp))
                Text("(Pictures/DZlog/ 하위에 저장된 사진이 있어야 표시됩니다.)")
                return@Column
            }

            if (isSelectionMode) {
                SelectionTopBar(selectedCount = selectedNodes.size)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .padding(bottom = bottomInset)
            ) {
                rootNode?.takeIf { node -> node.waterCount + node.originalCount > 0 }?.let { node ->
                    item(key = "dzlog-root") {
                        val summary = LogGroupSummary(
                            name = node.name,
                            photoCount = node.waterCount + node.originalCount,
                            latestDateAddedSeconds = node.latestDateAddedSeconds,
                            latestContentUri = node.latestContentUri,
                        )
                        val selected = selectedNodes.contains(DzlogMediaStoreReader.ROOT_G1)
                        LogGroupCard(
                            summary = summary,
                            titleOverride = DzlogMediaStoreReader.ROOT_G1,
                            isRootHighlight = true,
                            isSelected = selected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedNodes = if (selected) {
                                                selectedNodes - DzlogMediaStoreReader.ROOT_G1
                                            } else {
                                                selectedNodes + DzlogMediaStoreReader.ROOT_G1
                                            }
                                        } else {
                                            onOpenGridByRuleC(
                                                DzlogMediaStoreReader.ROOT_G1,
                                                node.waterRel,
                                                node.originalRel,
                                                node.waterCount,
                                                node.originalCount,
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        isSelectionMode = true
                                        selectedNodes = selectedNodes + DzlogMediaStoreReader.ROOT_G1
                                    }
                                )
                        )
                    }
                }
                items(g1Nodes, key = { it.name }) { node ->
                    val summary = LogGroupSummary(
                        name = node.name,
                        photoCount = node.waterCount + node.originalCount,
                        latestDateAddedSeconds = node.latestDateAddedSeconds,
                        latestContentUri = node.latestContentUri,
                    )
                    val selected = selectedNodes.contains(node.name)
                    LogGroupCard(
                        summary = summary,
                        titleOverride = node.name,
                        isSelected = selected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedNodes = if (selected) selectedNodes - node.name else selectedNodes + node.name
                                    } else if (node.hasG2) {
                                        onOpenG2(node.name)
                                    } else {
                                        onOpenGridByRuleC(
                                            node.name,
                                            node.waterRel,
                                            node.originalRel,
                                            node.waterCount,
                                            node.originalCount,
                                        )
                                    }
                                },
                                onLongClick = {
                                    isSelectionMode = true
                                    selectedNodes = selectedNodes + node.name
                                }
                            )
                    )
                }
            }
        }

        if (isSelectionMode) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                SelectionBottomBar(
                    onClose = { resetSelection() },
                    onSelectAll = {
                        selectedNodes = buildSet {
                            if (rootNode != null) add(DzlogMediaStoreReader.ROOT_G1)
                            addAll(g1Nodes.map { it.name })
                        }
                    },
                    onShare = { },
                    shareEnabled = false,
                    onDelete = if (selectedNodes.isNotEmpty()) {
                        { prepareDeleteRequest() }
                    } else null,
                )
            }
        }
    }

}
