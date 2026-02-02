package com.example.dzlog.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dzlog.data.log.LogEntity
import com.example.dzlog.data.log.LogRepository
import com.example.dzlog.ui.common.DDZCard
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LogListScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { LogRepository.getInstance(context) }

    var items by remember { mutableStateOf<List<LogEntity>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { repository.listAll() }
        }.onSuccess {
            items = it
            error = null
        }.onFailure { e ->
            error = e.message ?: "불러오기 실패"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(DDZSpacing.screenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("앱 내 로그", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(DDZSpacing.sectionGap))

        if (error != null) {
            Text("오류: $error", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            return@Column
        }

        if (items.isEmpty()) {
            Text("저장된 DZlog 결과물이 없습니다.", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            Spacer(Modifier.height(DDZSpacing.itemGap))
            Text("(앞으로 저장되는 항목만 표시됩니다.)", style = DDZTypography.Caption, color = DDZColor.TextMuted)
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)
        ) {
            items(items, key = { it.id }) { log ->
                LogListItem(
                    log = log,
                    onClick = { onOpenDetail(log.id) }
                )
            }
        }
    }
}

@Composable
private fun LogListItem(
    log: LogEntity,
    onClick: () -> Unit
) {
    DDZCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(72.dp)) {
                DzThumbnail(uriString = log.imageUri)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(log.fileName, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    dzFormatDate(log.createdAt / 1000L),
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted
                )
                val path = log.relativePath
                if (!path.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(path, style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
            }
        }
    }
}
