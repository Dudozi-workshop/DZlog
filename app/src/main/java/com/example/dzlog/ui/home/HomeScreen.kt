package com.example.dzlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.data.mediastore.DzlogMediaStoreReader
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.MediaImageItem
import com.example.dzlog.ui.common.DisplayTablePreview
import com.example.dzlog.ui.log.DzThumbnail
import com.example.dzlog.ui.log.dzFormatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

@Composable
fun HomeScreen(
    tableTemplateState: TableTemplateState,
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenLogFor: (g1: String, g2: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by AppSettingsStore.flow(context).collectAsState(
        initial = com.example.dzlog.data.datastore.AppSettings(
            saveMode = com.example.dzlog.domain.model.SaveMode.BOTH,
            continuousPreviewMode = com.example.dzlog.domain.model.ContinuousPreviewMode.OFF,
            counterPadding = 0,
            counterSuffixEnabled = true,
            resetCounterOnPathChange = true,
            toastEnabled = true,
            hapticEnabled = true,
            blankWarningEnabled = true,
            usedCounterValuesJson = null
        )
    )

    var latest by remember { mutableStateOf<MediaImageItem?>(null) }
    LaunchedEffect(Unit) {
        latest = withContext(Dispatchers.IO) {
            runCatching { DzlogMediaStoreReader(context.contentResolver).loadLatestImage() }.getOrNull()
        }
    }

    fun extractG1G2(relativePath: String): Pair<String, String> {
        val norm = if (relativePath.endsWith('/')) relativePath else "$relativePath/"
        val prefix = "Pictures/DZlog/"
        if (!norm.startsWith(prefix)) return DzlogMediaStoreReader.DEFAULT_G1 to DzlogMediaStoreReader.DEFAULT_G2
        val rest = norm.removePrefix(prefix).trim('/' )
        if (rest.isBlank()) return DzlogMediaStoreReader.DEFAULT_G1 to DzlogMediaStoreReader.DEFAULT_G2
        val parts = rest.split('/').filter { it.isNotBlank() }
        val g1 = parts.getOrNull(0) ?: DzlogMediaStoreReader.DEFAULT_G1
        val g2 = parts.getOrNull(1) ?: DzlogMediaStoreReader.DEFAULT_G2
        return g1 to g2
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0D))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("DZlog", fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(14.dp))

            Button(onClick = onStartCamera, modifier = Modifier.fillMaxWidth()) { Text("촬영 시작") }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpenLog, modifier = Modifier.fillMaxWidth()) { Text("앱 내 로그") }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("설정") }

            Spacer(Modifier.weight(1f))

            // 하단: 6:4 (표 상세설정 / 최근 로그)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                Card(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .clickable(onClick = onOpenTableEditor)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("표 상세설정", fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            DisplayTablePreview(
                                templateState = tableTemplateState,
                                counterDigits = settings.counterPadding,
                                now = Date(),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(0.04f))

                Card(
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxHeight()
                        .clickable {
                            val it = latest
                            if (it != null) {
                                val (g1, g2) = extractG1G2(it.relativePath)
                                onOpenLogFor(g1, g2)
                            } else {
                                onOpenLog()
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("최근 로그", fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        val it = latest
                        if (it == null) {
                            Text("최근 항목 없음", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)) {
                                DzThumbnail(it.uri.toString())
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(dzFormatDate(it.dateAddedSeconds), fontSize = 11.sp, color = Color.Gray)
                            val (g1, g2) = extractG1G2(it.relativePath)
                            Text("$g1 / $g2", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    tableTemplateStateProvider: () -> com.example.dzlog.domain.model.TableTemplateState,
    onBack: () -> Unit,
    onOpenTableDetail: () -> Unit,
    onOpenCaptureSettings: () -> Unit
) {
    com.example.dzlog.ui.settings.SettingsRootScreen(
        tableTemplateStateProvider = tableTemplateStateProvider,
        onBack = onBack,
        onOpenTableDetail = onOpenTableDetail,
        onOpenCaptureSettings = onOpenCaptureSettings
    )
}
