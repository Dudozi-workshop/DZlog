package com.dudoziworkshop.dzlog.ui.log

import android.widget.Toast
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
import com.dudoziworkshop.dzlog.feature.log.policy.launchMediaDeleteRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogG2Screen(
    g1: String,
    onBack: () -> Unit,
    onOpenGridForRelativePath: (g2Label: String, relativePath: String, originalRelativePath: String?) -> Unit,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val reader = remember { DzlogMediaStoreReader(resolver) }

    var groupRootNode by remember { mutableStateOf<DzlogMediaStoreReader.G2Node?>(null) }
    var g2Nodes by remember { mutableStateOf<List<DzlogMediaStoreReader.G2Node>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedG2 by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun resetSelection() {
        isSelectionMode = false
        selectedG2 = emptySet()
    }

    fun toSummary(node: DzlogMediaStoreReader.G2Node): LogGroupSummary {
        return LogGroupSummary(
            name = node.label,
            photoCount = node.waterCount + node.originalCount,
            latestDateAddedSeconds = node.latestDateAddedSeconds,
            latestContentUri = node.latestContentUri,
        )
    }

    fun openByCounts(
        label: String,
        waterRel: String,
        originalRel: String,
        waterCount: Int,
        originalCount: Int,
    ) {
        when {
            waterCount > 0 -> onOpenGridForRelativePath(label, waterRel, originalRel)
            originalCount > 0 -> onOpenGridForRelativePath(label, originalRel, null)
            else -> Toast.makeText(context, "사진이 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    fun reload() {
        isLoading = true
        runCatching { reader.loadG2Nodes(g1) }
            .onSuccess { (rootNode, nodes) ->
                groupRootNode = rootNode
                g2Nodes = nodes
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

        if (g2Nodes.isEmpty() && groupRootNode == null) {
            Text("아직 사진이 없습니다.")
            return@Column
        }

        if (isSelectionMode) {
            SelectionTopBar(selectedCount = selectedG2.size)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupRootNode?.let { node ->
                item(key = "group-root") {
                    LogGroupTileCard(
                        summary = toSummary(node),
                        isSelected = false,
                        isGroupRootHighlight = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    openByCounts(
                                        label = node.label,
                                        waterRel = node.waterRel,
                                        originalRel = node.originalRel,
                                        waterCount = node.waterCount,
                                        originalCount = node.originalCount,
                                    )
                                },
                                onLongClick = { }
                            )
                    )
                }
            }

            items(g2Nodes, key = { it.label }) { node ->
                val selected = selectedG2.contains(node.label)
                LogGroupTileCard(
                    summary = toSummary(node),
                    isSelected = selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    selectedG2 = if (selected) selectedG2 - node.label else selectedG2 + node.label
                                } else {
                                    openByCounts(
                                        label = node.label,
                                        waterRel = node.waterRel,
                                        originalRel = node.originalRel,
                                        waterCount = node.waterCount,
                                        originalCount = node.originalCount,
                                    )
                                }
                            },
                            onLongClick = {
                                isSelectionMode = true
                                selectedG2 = selectedG2 + node.label
                            }
                        )
                )
            }
        }

        if (isSelectionMode) {
            SelectionBottomBar(
                onClose = { resetSelection() },
                onSelectAll = {
                    selectedG2 = g2Nodes.map { it.label }.toSet()
                },
                onShare = { },
                shareEnabled = false,
                onDelete = if (selectedG2.isNotEmpty()) {
                    {
                        val allUris = mutableListOf<android.net.Uri>()
                        g2Nodes.filter { selectedG2.contains(it.label) }.forEach { node ->
                            allUris.addAll(reader.loadImages(node.waterRel).map { it.uri })
                            allUris.addAll(reader.loadImages(node.originalRel).map { it.uri })
                        }
                        startDeleteRequest(allUris)
                    }
                } else null
            )
        }
    }
}
