package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Arrangement
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
    val reader = remember { DzlogMediaStoreReader(context.contentResolver) }

    var rootNode by remember { mutableStateOf<DzlogMediaStoreReader.G1Node?>(null) }
    var g1Nodes by remember { mutableStateOf<List<DzlogMediaStoreReader.G1Node>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("앨범")
            Button(onClick = onBack) { Text(stringResource(R.string.action_back)) }
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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            rootNode?.takeIf { node -> node.waterCount + node.originalCount > 0 }?.let { node ->
                item(key = "dzlog-root") {
                    val summary = LogGroupSummary(
                        name = node.name,
                        photoCount = node.waterCount + node.originalCount,
                        latestDateAddedSeconds = node.latestDateAddedSeconds,
                        latestContentUri = node.latestContentUri,
                    )
                    LogGroupCard(
                        summary = summary,
                        titleOverride = DzlogMediaStoreReader.ROOT_G1,
                        isRootHighlight = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                onOpenGridByRuleC(
                                    DzlogMediaStoreReader.ROOT_G1,
                                    node.waterRel,
                                    node.originalRel,
                                    node.waterCount,
                                    node.originalCount,
                                )
                            }
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
                LogGroupCard(
                    summary = summary,
                    titleOverride = node.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            if (node.hasG2) {
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
                        }
                )
            }
        }
    }
}
