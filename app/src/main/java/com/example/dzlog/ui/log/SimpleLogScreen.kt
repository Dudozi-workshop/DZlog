package com.example.dzlog.ui.log

import android.content.ContentUris
import android.provider.MediaStore
import android.widget.ImageView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SimpleLogItem(
    val uri: android.net.Uri,
    val relativePath: String,
    val dateAddedMillis: Long
)

@Composable
fun SimpleLogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<SimpleLogItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )
        // ✅ 파일명(카운터 등)과 무관하게 "폴더" 기준으로만 로드
        val selection = (
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND " +
        "${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?"
        )
        val args = arrayOf("Pictures/DZlog/%", "%/original/%")

        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val out = mutableListOf<SimpleLogItem>()
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            sort
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val relIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (c.moveToNext() && out.size < 100) {
                val id = c.getLong(idIdx)
                val rel = c.getString(relIdx).orEmpty()
                val dateAdded = c.getLong(dateIdx) * 1000L
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                out.add(SimpleLogItem(uri = uri, relativePath = rel, dateAddedMillis = dateAdded))
            }
        }
        items = out
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DDZColor.Background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("로그", color = DDZColor.TextPrimary, style = DDZTypography.ScreenTitle)
            Spacer(Modifier.weight(1f))
            Button(onClick = onBack) { Text("뒤로") }
        }

        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("기록이 없습니다", color = DDZColor.TextMuted, style = DDZTypography.Body)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    LogRow(item)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun LogRow(item: SimpleLogItem) {
    val dateText = remember(item.dateAddedMillis) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.dateAddedMillis))
    }
    val projectText = remember(item.relativePath) {
        deriveG1G2FromRelativePath(item.relativePath)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DDZColor.Card)
            ) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    update = { iv ->
                        iv.setImageURI(item.uri)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(dateText, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(projectText, style = DDZTypography.Caption, color = DDZColor.TextMuted)
            }
        }
    }
}

private fun deriveG1G2FromRelativePath(relativePath: String): String {
    val norm = relativePath.replace("\\", "/")
    val idx = norm.indexOf("DZlog/")
    if (idx < 0) return "-"
    val tail = norm.substring(idx + "DZlog/".length).trim('/').trim()
    if (tail.isBlank()) return "-"
    val parts = tail.split("/").filter { it.isNotBlank() }
    val g1 = parts.getOrNull(0) ?: "-"
    val g2 = parts.getOrNull(1)
    return if (g2.isNullOrBlank()) g1 else "$g1 / $g2"
}
