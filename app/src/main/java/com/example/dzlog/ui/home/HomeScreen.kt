package com.example.dzlog.ui.home

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.example.dzlog.data.preferences.KEY_WM_LABEL_SCALE
import com.example.dzlog.data.preferences.KEY_WM_OFFSET_X
import com.example.dzlog.data.preferences.KEY_WM_OFFSET_Y
import com.example.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenLogs: () -> Unit,
    tableTemplateState: TableTemplateState
) {
    val context = LocalContext.current

    // ---- 최근 로그(가장 최근 DZlog 결과물 1장) ----
    var latestUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var latestDateText by remember { mutableStateOf("-") }
    var latestProjectText by remember { mutableStateOf("-") }

    fun reloadLatest() {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Images.Media.RELATIVE_PATH} NOT LIKE ?"
        val args = arrayOf("%DZlog/%", "%/original/%")
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            sort
        )?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(0)
                val rel = c.getString(1).orEmpty()
                val dateAdded = c.getLong(2) * 1000L

                latestUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                latestDateText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateAdded))
                latestProjectText = deriveG1G2FromRelativePath(rel)
            } else {
                latestUri = null
                latestDateText = "-"
                latestProjectText = "-"
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadLatest()
    }

    // ---- 표 전체 미리보기(축소 렌더) ----
    var tablePreviewBmp by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(tableTemplateState) {
        tablePreviewBmp = buildTablePreviewBitmap(context, tableTemplateState)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Text("DZlog", fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(20.dp))

            Button(onClick = onStartCamera, modifier = Modifier.fillMaxWidth()) {
                Text("촬영 시작")
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("설정")
            }
            Spacer(Modifier.height(12.dp))

            // --- 하단: 7:3 한 줄 (현재 표 / 최근 로그) ---
            Row(modifier = Modifier.fillMaxWidth()) {
                // 7: 현재 표(전체 축소 미리보기)
                Card(
                    modifier = Modifier
                        .weight(0.7f)
                        .height(140.dp)
                        .clickable(onClick = onOpenTableEditor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CURRENT TABLE", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141517)),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = tablePreviewBmp
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "table preview",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text("미리보기 생성중", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(0.03f))

                // 3: 최근 로그(대표 사진 타일)
                Card(
                    modifier = Modifier
                        .weight(0.27f)
                        .height(140.dp)
                        .clickable {
                            // MVP: 전체 로그로 연결 (뷰어는 로그 화면에서 확장)
                            onOpenLogs()
                        },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141517)),
                            contentAlignment = Alignment.Center
                        ) {
                            val uri = latestUri
                            if (uri != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            scaleType = ImageView.ScaleType.CENTER_CROP
                                        }
                                    },
                                    update = { iv ->
                                        iv.setImageURI(uri)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("최근\n기록 없음", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(latestDateText, fontSize = 10.sp, color = Color.White)
                        Text(latestProjectText, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(onClick = onOpenLogs, modifier = Modifier.fillMaxWidth()) {
                Text("전체 로그 보기")
            }
        }
    }
}

private fun deriveG1G2FromRelativePath(relativePath: String): String {
    // 예: "Pictures/DZlog/G1/G2/" 또는 "Pictures/DZlog/G1/"
    val norm = relativePath.replace("\\\\", "/")
    val idx = norm.indexOf("DZlog/")
    if (idx < 0) return "-"
    val tail = norm.substring(idx + "DZlog/".length).trim('/').trim()
    if (tail.isBlank()) return "-"
    val parts = tail.split("/").filter { it.isNotBlank() }
    val g1 = parts.getOrNull(0) ?: "-"
    val g2 = parts.getOrNull(1)
    return if (g2.isNullOrBlank()) g1 else "$g1 / $g2"
}

private suspend fun buildTablePreviewBitmap(
    context: android.content.Context,
    template: TableTemplateState
): Bitmap? {
    return runCatching {
        val prefs = context.dataStore.data.first()

        val anchor = when (prefs[KEY_WM_TABLE_ANCHOR] ?: 3) {
            0 -> WatermarkTableAnchor.TOP_LEFT
            1 -> WatermarkTableAnchor.TOP_RIGHT
            2 -> WatermarkTableAnchor.BOTTOM_LEFT
            3 -> WatermarkTableAnchor.BOTTOM_RIGHT
            else -> WatermarkTableAnchor.CUSTOM
        }

        val tableWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(40, 100)
        val tableHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 35)
        val offsetXRatio = (prefs[KEY_WM_OFFSET_X] ?: 0).coerceIn(0, 100)
        val offsetYRatio = (prefs[KEY_WM_OFFSET_Y] ?: 0).coerceIn(0, 100)

        val bgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255)
        val labelScale = (prefs[KEY_WM_LABEL_SCALE] ?: 100).coerceIn(60, 160)
        val valueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)

        val counterDigits = (prefs[KEY_COUNTER_DIGITS] ?: 4).coerceIn(1, 6)

        val resolver = TableResolver()
        val plan = resolver.plan(
            cells = template.cells,
            captureNow = Date(),
            config = TableResolver.Config(
                counterDigits = counterDigits,
                dateFormat = "yyyy.MM.dd",
                timeFormat = "HH.mm.ss"
            )
        )
        val wmCells = WatermarkBuilder.buildTableCells(plan.resolvedCells)

        // 프리뷰용 캔버스 (너무 작으면 글자 뭉개지므로 적당히 크게 만든 뒤 Compose에서 축소)
        val w = 900
        val h = 420
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())

        drawWatermarkTableOnCanvas(
            canvas = canvas,
            bounds = bounds,
            cells = wmCells,
            rows = template.rows,
            cols = template.cols,
            showLabel = false,
            anchor = anchor,
            offsetXRatio = offsetXRatio,
            offsetYRatio = offsetYRatio,
            tableHeightRatio = tableHeightRatio,
            tableWidthRatio = tableWidthRatio,
            bgAlpha = bgAlpha,
            labelScale = labelScale,
            valueScale = valueScale
        )

        bmp
    }.getOrNull()
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("설정 화면(준비중)")
        Button(onClick = onBack) { Text("Back") }
    }
}
