package com.example.dzlog.ui.log

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dzlog.data.mediastore.DzlogMediaStoreReader
import com.example.dzlog.domain.model.MediaImageItem

@Composable
fun LogGridScreen(
    g1: String,
    g2: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val reader = remember { DzlogMediaStoreReader(context.contentResolver) }

    var items by remember { mutableStateOf<List<MediaImageItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<MediaImageItem?>(null) }

    val relativePath = remember(g1, g2) { buildRelativePathFromG1G2(g1, g2) }

    LaunchedEffect(relativePath) {
        runCatching { reader.loadImages(relativePath) }
            .onSuccess {
                items = it
                error = null
            }
            .onFailure { e ->
                error = e.message ?: "불러오기 실패"
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DZlog / $g1 / $g2")
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text("오류: $error")
            return@Column
        }

        if (items.isEmpty()) {
            Text("사진이 없습니다.")
            Text("(결과물만 표시되며 original/ 폴더는 제외됩니다.)")
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = item }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Thumbnail(uriString = item.uri.toString())
                    }
                }
            }
        }
    }

    if (selected != null) {
        val item = selected!!
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.displayName) },
            text = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LargePreview(uriString = item.uri.toString())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(item.relativePath)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shareImage(context = context, item = item)
                    }
                ) { Text("공유") }
            },
            dismissButton = {
                Button(onClick = { selected = null }) { Text("닫기") }
            }
        )
    }
}

private fun buildRelativePathFromG1G2(g1: String, g2: String): String {
    val DEFAULT = "(기본)"
    return when {
        g1 == DEFAULT -> "Pictures/DZlog/"
        g2 == DEFAULT -> "Pictures/DZlog/$g1/"
        else -> "Pictures/DZlog/$g1/$g2/"
    }
}

@Composable
private fun Thumbnail(uriString: String) {
    val context = LocalContext.current
    var bmp by remember(uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uriString) {
        bmp = runCatching { decodeSampledBitmap(context, uriString, reqSize = 260) }.getOrNull()
    }

    if (bmp != null) {
        Image(
            bitmap = bmp!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
    } else {
        Text("...")
    }
}

@Composable
private fun LargePreview(uriString: String) {
    val context = LocalContext.current
    var bmp by remember(uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uriString) {
        bmp = runCatching { decodeSampledBitmap(context, uriString, reqSize = 1200) }.getOrNull()
    }

    if (bmp != null) {
        Image(
            bitmap = bmp!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Text("미리보기 로딩중...")
    }
}

private fun decodeSampledBitmap(
    context: android.content.Context,
    uriString: String,
    reqSize: Int
): android.graphics.Bitmap {
    val uri = android.net.Uri.parse(uriString)

    // 1) bounds
    val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, optsBounds)
    }

    // 2) sample
    val (w, h) = optsBounds.outWidth to optsBounds.outHeight
    var inSampleSize = 1
    if (w > reqSize || h > reqSize) {
        var halfW = w / 2
        var halfH = h / 2
        while (halfW / inSampleSize >= reqSize && halfH / inSampleSize >= reqSize) {
            inSampleSize *= 2
        }
    }

    val opts = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        this.inSampleSize = inSampleSize
    }

    context.contentResolver.openInputStream(uri)?.use { input ->
        return BitmapFactory.decodeStream(input, null, opts)
            ?: throw IllegalStateException("decode failed")
    }
    throw IllegalStateException("openInputStream failed")
}

private fun shareImage(context: android.content.Context, item: MediaImageItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "공유"))
}
