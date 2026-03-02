package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.LogGroupSummary
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogG1Screen(
    onGoHome: () -> Unit,
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

    var rootNode by remember { mutableStateOf<DzlogMediaStoreReader.G1Node?>(null) }
    var g1Nodes by remember { mutableStateOf<List<DzlogMediaStoreReader.G1Node>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(Unit) {
        reload()
    }

    Box(
        modifier = Modifier
            .dzScreen()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onGoHome,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = DDZColor.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = "DZlog",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DDZColor.Primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Box(modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
                return@Column
            }

            if (error != null) {
                Text("오류: $error")
                Spacer(Modifier.height(12.dp))
                return@Column
            }

            if (g1Nodes.isEmpty() && rootNode == null) {
                Text("저장된 DZlog 결과물이 없습니다.")
                Spacer(Modifier.height(6.dp))
                Text("(Pictures/DZlog/ 하위에 저장된 사진이 있어야 표시됩니다.)")
                return@Column
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
            ) {
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
                                .combinedClickable(
                                    onClick = {
                                        onOpenGridByRuleC(
                                            DzlogMediaStoreReader.ROOT_G1,
                                            node.waterRel,
                                            node.originalRel,
                                            node.waterCount,
                                            node.originalCount,
                                        )
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
                    LogGroupCard(
                        summary = summary,
                        titleOverride = node.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
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
                    )
                }
            }
        }
    }

}
