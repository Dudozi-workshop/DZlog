package com.dudoziworkshop.dzlog.poc

import androidx.compose.runtime.*
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorDocumentCodec
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.*
import com.dudoziworkshop.dzlog.ui.table.mock.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import platform.Foundation.*

internal const val IOS_EDITOR_KEY = "dzlog.ios.editor.v1"
internal const val IOS_EDITOR_TEST_KEY = "dzlog.ios.editor.uitest.v1"

/** One atomic preference value in this iOS app's sandbox; no Android store or photos are touched. */
internal class IosEditorStore(private val key: String) {
    fun load(): EditorDocument? = NSUserDefaults.standardUserDefaults.stringForKey(key)?.let(EditorDocumentCodec::decode)
    fun save(value: EditorDocument): Boolean = runCatching {
        val encoded = EditorDocumentCodec.encode(value)
        val decoded = EditorDocumentCodec.decode(encoded)
        check(decoded.template.id == value.template.id && decoded.nextCounter == value.nextCounter) {
            "편집 내용을 확인하지 못했습니다."
        }
        NSUserDefaults.standardUserDefaults.setObject(encoded, forKey = key)
        NSUserDefaults.standardUserDefaults.synchronize()
    }.getOrDefault(false)
    fun resetTestFixture() {
        check(key == IOS_EDITOR_TEST_KEY)
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}

internal class IosTableEditorPlatform(private val onNotice: (String) -> Unit) : TableEditorPlatform {
    @Composable override fun Preview(session: TableEditorV2SessionState): PreviewState {
        val now by produceState(initialValue = tableEditorNow()) {
            while (isActive) {
                delay(computeNextDelayMillis(TickUnit.MINUTE))
                value = tableEditorNow()
            }
        }
        return buildPreview(PreviewInput(session.finalTemplateForSave(), now, session.draftCounterPadding,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, session.saveRulesDraft.includePathInScope,
            session.saveRulesDraft.includeFilenameInScope, session.draftSaveMode, session.draftNextCounter, 0))
    }

    @Composable override fun Counter(session: TableEditorV2SessionState, active: Boolean): TableEditorCounterActions {
        val actions = remember { IosDraftCounterActions() }
        LaunchedEffect(active, session.draftNextCounter, session.draftSaveMode, session.draftCounterPadding,
            session.saveRulesDraft.includePathInScope, session.saveRulesDraft.includeFilenameInScope,
            session.draftTemplateState) {
            if (active && session.draftNextCounter == null) actions.initialize(session)
        }
        return actions
    }
    // The common top-bar/back decision handles iOS navigation. Android still installs its system BackHandler.
    @Composable override fun HandleBack(onBack: () -> Unit) = Unit
    override fun notify(message: String) = onNotice(message)
}

private class IosDraftCounterActions : TableEditorCounterActions {
    private val repository = PhotoKitCaptureMediaRepository()
    override var busy by mutableStateOf(false)
        private set
    override var status by mutableStateOf<String?>(null)
        private set

    private fun snapshot(session: TableEditorV2SessionState) = session.currentSnapshot()

    suspend fun initialize(session: TableEditorV2SessionState) {
        if (busy) return
        val before = snapshot(session)
        busy = true
        try {
            val next = readSavedNext(session, 1)
            if (before == snapshot(session)) session.initializeCounterState(next, true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (before == snapshot(session)) status = "저장된 번호를 확인하지 못했습니다."
        } finally {
            busy = false
        }
    }

    override suspend fun sync(session: TableEditorV2SessionState) {
        if (busy) return
        val before = snapshot(session)
        busy = true
        try {
            val next = readSavedNext(session, session.draftNextCounter ?: 1)
            if (before == snapshot(session)) {
                session.commitNextCounterChange(next, true)
                status = "저장 이력 기준으로 ${formatMockCounter(next, session.draftCounterPadding)}부터 이어집니다."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (before == snapshot(session)) status = "저장된 번호를 확인하지 못했습니다."
        } finally {
            busy = false
        }
    }

    private suspend fun readSavedNext(session: TableEditorV2SessionState, current: Int): Int {
        val scope = buildScope(CaptureScopeInput(
            templateState = session.finalTemplateForSave(),
            captureNow = tableEditorNow(),
            counterDigits = session.draftCounterPadding,
            dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
            timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            includeFilenameInCounterScope = session.saveRulesDraft.includeFilenameInScope,
            saveMode = session.draftSaveMode,
            phraseProgressCursor = 0,
        ))
        repository.ensureWritePermission()
        return repository.resolveStableAutomaticNext(
            relativePathKey = if (session.saveRulesDraft.includePathInScope) {
                scope.counterScope.relativePathKey
            } else "*",
            scanPrefix = scope.scanPrefix,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            currentNext = current,
        )
    }

    override fun edit(session: TableEditorV2SessionState, next: Int) {
        session.commitNextCounterChange(next, false)
        status = null
    }

    override fun reset(session: TableEditorV2SessionState) {
        session.commitNextCounterChange(1, false)
        status = "다음 번호를 ${formatMockCounter(1, session.draftCounterPadding)}로 변경했습니다."
    }
}

/** New collection key; the validated v1 editor value remains untouched as a recovery source. */
internal class IosWorkspaceStore(private val legacyKey: String) {
    private val workspaceKey = "$legacyKey.workspace.v2"
    fun load(seed: com.dudoziworkshop.dzlog.data.template.SavedTableTemplate): com.dudoziworkshop.dzlog.data.template.EditorWorkspace {
        NSUserDefaults.standardUserDefaults.stringForKey(workspaceKey)?.let {
            return com.dudoziworkshop.dzlog.data.template.EditorWorkspaceCodec.decode(it)
        }
        val legacy = IosEditorStore(legacyKey).load()
        return com.dudoziworkshop.dzlog.data.template.editorWorkspaceFromLegacy(legacy, seed)
    }
    fun save(value: com.dudoziworkshop.dzlog.data.template.EditorWorkspace): Boolean = runCatching {
        val encoded = com.dudoziworkshop.dzlog.data.template.EditorWorkspaceCodec.encode(value)
        val decoded = com.dudoziworkshop.dzlog.data.template.EditorWorkspaceCodec.decode(encoded)
        check(decoded.documents.map { it.template.id } == value.documents.map { it.template.id })
        check(decoded.activeTemplateId == value.activeTemplateId && decoded.settings == value.settings)
        check(decoded.documents.zip(value.documents).all { (a, b) ->
            a.nextCounter == b.nextCounter && a.usesAutoNext == b.usesAutoNext &&
                a.counterPadding == b.counterPadding && a.saveMode == b.saveMode &&
                a.capturePlacement == b.capturePlacement &&
                a.includePathInScope == b.includePathInScope && a.includeFilenameInScope == b.includeFilenameInScope
        })
        NSUserDefaults.standardUserDefaults.setObject(encoded, forKey = workspaceKey)
        NSUserDefaults.standardUserDefaults.synchronize()
    }.getOrDefault(false)
    fun resetTestFixture() {
        check(legacyKey == IOS_EDITOR_TEST_KEY || legacyKey == IOS_NAVIGATION_TEST_KEY)
        NSUserDefaults.standardUserDefaults.removeObjectForKey(workspaceKey)
        NSUserDefaults.standardUserDefaults.removeObjectForKey(legacyKey)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}
internal const val IOS_NAVIGATION_TEST_KEY = "dzlog.ios.navigation.uitest.v1"

