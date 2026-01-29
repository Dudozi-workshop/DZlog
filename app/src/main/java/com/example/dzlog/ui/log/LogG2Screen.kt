package com.example.dzlog.ui.log

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
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

@Composable
fun LogG2Screen(
    g1: String,
    onBack: () -> Unit,
    onSelectG2: (String) -> Unit
) {
    val context = LocalContext.current
    val reader = remember { DzlogMediaStoreReader(context.contentResolver) }

    var g2List by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(g1) {
        runCatching { reader.loadG2List(g1) }
            .onSuccess {
                g2List = it
                error = null
            }
            .onFailure { e ->
                error = e.message ?: "불러오기 실패"
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DZlog / $g1")
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text("오류: $error")
            return@Column
        }

        if (g2List.isEmpty()) {
            Text("하위 폴더가 없습니다.")
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(g2List) { g2 ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSelectG2(g2) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(g2)
                        Spacer(Modifier.height(4.dp))
                        Text("탭해서 사진 보기")
                    }
                }
            }
        }
    }
}
