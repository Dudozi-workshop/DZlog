package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.preview.PreviewState
import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import com.dudoziworkshop.dzlog.domain.time.formatCaptureTime

interface TableEditorPlatform {
    @Composable fun Preview(session: TableEditorV2SessionState): PreviewState
    @Composable fun Counter(session: TableEditorV2SessionState, active: Boolean): TableEditorCounterActions
    @Composable fun HandleBack(onBack: () -> Unit)
    fun notify(message: String)
}

interface TableEditorCounterActions {
    val busy: Boolean
    val status: String?
    val canSync: Boolean get() = true
    fun edit(session: TableEditorV2SessionState, next: Int)
    fun reset(session: TableEditorV2SessionState)
    suspend fun sync(session: TableEditorV2SessionState)
}

expect fun tableEditorNow(): CaptureTime

fun editorDateExample(pattern: String): String =
    runCatching { formatCaptureTime(pattern, tableEditorNow()) }.getOrElse { "" }
