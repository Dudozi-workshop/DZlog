package com.dudoziworkshop.dzlog.ui.table.mock

import android.content.Context
import androidx.compose.runtime.*
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.feature.counter.table.SaveSettingsCounterController
import kotlinx.coroutines.CancellationException
import java.util.Date

internal class TableEditorCounterOrchestrator {
    var busy by mutableStateOf(false)
        private set
    var status by mutableStateOf<String?>(null)
        private set

    private fun streamSnapshot(session: TableEditorV2SessionState) = session.currentSnapshot().copy(
        nextCounter = null, usesAutoNext = true,
    )

    suspend fun initialize(context: Context, session: TableEditorV2SessionState) {
        val snapshot = streamSnapshot(session)
        busy = true
        status = null
        try {
            val state = SaveSettingsCounterController.readCurrent(context, session.finalTemplateForSave(),
                snapshot.saveMode, snapshot.counterPadding, snapshot.includePathInCounterScope,
                snapshot.includeFilenameInCounterScope)
            if (snapshot == streamSnapshot(session)) session.initializeCounterState(state.next, state.usesAutoNext)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (snapshot == streamSnapshot(session)) status = "저장된 번호를 확인하지 못했습니다."
        } finally {
            busy = false
        }
    }

    suspend fun sync(context: Context, session: TableEditorV2SessionState) {
        if (busy) return
        val snapshot = streamSnapshot(session)
        busy = true
        try {
            val next = SaveSettingsCounterController.readSavedImageNext(context, session.finalTemplateForSave(),
                snapshot.saveMode, snapshot.counterPadding, snapshot.includePathInCounterScope,
                snapshot.includeFilenameInCounterScope)
            if (snapshot == streamSnapshot(session)) {
                session.commitNextCounterChange(next, usesAutoNext = true)
                status = "저장 이력 기준으로 ${formatMockCounter(next, snapshot.counterPadding)}부터 이어집니다."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (snapshot == streamSnapshot(session)) status = "저장된 번호를 확인하지 못했습니다."
        } finally {
            busy = false
        }
    }

    fun edit(session: TableEditorV2SessionState, next: Int) {
        session.commitNextCounterChange(next, usesAutoNext = false)
        status = null
    }

    fun reset(session: TableEditorV2SessionState) {
        session.commitNextCounterChange(1, usesAutoNext = false)
        status = "다음 번호를 ${formatMockCounter(1, session.draftCounterPadding)}로 변경했습니다."
    }
}

@Composable
internal fun rememberTableEditorCounterOrchestrator(context: Context, session: TableEditorV2SessionState,
    active: Boolean): TableEditorCounterOrchestrator {
    val controller = remember { TableEditorCounterOrchestrator() }
    LaunchedEffect(active, session.draftNextCounter, session.draftSaveMode, session.draftCounterPadding,
        session.saveRulesDraft.includePathInScope, session.saveRulesDraft.includeFilenameInScope,
        session.draftTemplateState) {
        if (active && session.draftNextCounter == null) controller.initialize(context, session)
    }
    return controller
}

@Composable
internal fun tableEditorNamingPreview(context: Context, session: TableEditorV2SessionState): Pair<String, String> {
    val settings by remember(context) { AppSettingsStore.flow(context) }.collectAsState(AppSettings.Default)
    val preview = buildPreview(PreviewInput(session.finalTemplateForSave(), Date(), session.draftCounterPadding,
        NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
        NamingFormatDefaults.FILE_NAME_DELIMITER, session.saveRulesDraft.includePathInScope,
        session.saveRulesDraft.includeFilenameInScope, session.draftSaveMode, session.draftNextCounter,
        settings.phraseProgressCursor))
    return preview.previewNaming.displayName to preview.previewNaming.relativePath
}
