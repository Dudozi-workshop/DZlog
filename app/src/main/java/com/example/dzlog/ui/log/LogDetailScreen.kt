package com.example.dzlog.ui.log

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dzlog.data.log.LogEntity
import com.example.dzlog.data.log.LogRepository
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LogDetailScreen(
    logId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { LogRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var logItem by remember { mutableStateOf<LogEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(logId) {
        runCatching {
            withContext(Dispatchers.IO) { repository.getById(logId) }
        }.onSuccess { item ->
            logItem = item
            error = null
        }.onFailure { e ->
            error = e.message ?: "불러오기 실패"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DDZColor.Surface)
                .padding(DDZSpacing.screenPadding),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("상세 보기", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(8.dp))

        if (error != null) {
            Text(
                "오류: $error",
                style = DDZTypography.Body,
                color = Color.White,
                modifier = Modifier.padding(DDZSpacing.screenPadding)
            )
            return@Column
        }

        val item = logItem
        if (item == null) {
            Text(
                "항목을 찾을 수 없습니다.",
                style = DDZTypography.Body,
                color = Color.White,
                modifier = Modifier.padding(DDZSpacing.screenPadding)
            )
            return@Column
        }

        DzFullImage(
            uriString = item.imageUri,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DDZColor.Surface)
                .padding(DDZSpacing.screenPadding),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { shareImage(context, item) }) { Text("공유") }
            Button(onClick = {
                scope.launch {
                    val deleted = withContext(Dispatchers.IO) {
                        repository.deleteLogWithImage(context, item)
                    }
                    if (deleted) {
                        onDeleted()
                    } else {
                        Toast.makeText(context, "이미지 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }) { Text("삭제") }
        }
    }
}

private fun shareImage(context: android.content.Context, item: LogEntity) {
    val uri = android.net.Uri.parse(item.imageUri)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "공유"))
}
