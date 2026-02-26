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
    onSelectG1: (String) -> Unit
) {
    val context = LocalContext.current
    val reader = remember { DzlogMediaStoreReader(context.contentResolver) }

    var g1Summaries by remember { mutableStateOf<List<LogGroupSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching { reader.loadG1Summaries() }
            .onSuccess {
                g1Summaries = it
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
                g1Summaries = emptyList()
            }) { Text("닫기") }
            return@Column
        }

        if (g1Summaries.isEmpty()) {
            Text("저장된 DZlog 결과물이 없습니다.")
            Spacer(Modifier.height(6.dp))
            Text("(Pictures/DZlog/ 하위에 저장된 사진이 있어야 표시됩니다.)")
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(g1Summaries, key = { it.name }) { summary ->
                LogGroupCard(
                    summary = summary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onSelectG1(summary.name) }
                )
            }
        }
    }
}
