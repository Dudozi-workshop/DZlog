package com.dudoziworkshop.dzlog.poc

import androidx.compose.runtime.*
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorDocumentCodec
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.*
import com.dudoziworkshop.dzlog.ui.table.mock.*
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
        LaunchedEffect(active) {
            if (active && session.draftNextCounter == null) session.initializeCounterState(1, false)
        }
        return actions
    }
    // The common top-bar/back decision handles iOS navigation. Android still installs its system BackHandler.
    @Composable override fun HandleBack(onBack: () -> Unit) = Unit
    override fun notify(message: String) = onNotice(message)
}

private class IosDraftCounterActions : TableEditorCounterActions {
    override val busy: Boolean = false
    override val canSync: Boolean = false
    override val status: String = "사진 기준 번호 동기화는 아직 연결 전입니다."
    override fun edit(session: TableEditorV2SessionState, next: Int) = session.commitNextCounterChange(next, false)
    override fun reset(session: TableEditorV2SessionState) = session.commitNextCounterChange(1, false)
    override suspend fun sync(session: TableEditorV2SessionState) = Unit
}
