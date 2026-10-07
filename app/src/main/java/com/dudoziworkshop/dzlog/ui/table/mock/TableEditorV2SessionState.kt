package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableUndoManager
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

internal data class MockEditorSnapshot(
    val templateState: TableTemplateState,
    val styleState: TableStyleState,
    val includePathInCounterScope: Boolean,
    val includeFilenameInCounterScope: Boolean,
)

internal class TableEditorV2SessionState(
    initialTemplateState: TableTemplateState,
    initialStyleState: TableStyleState,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
) {
    var draftTemplateState by mutableStateOf(initialTemplateState)
        private set

    var draftStyleState by mutableStateOf(initialStyleState)
        private set

    var saveRulesDraft by mutableStateOf(
        mockSaveRulesDraftFromTemplate(
            templateState = initialTemplateState,
            includePathInScope = includePathInCounterScope,
            includeFilenameInScope = includeFilenameInCounterScope,
        )
    )
        private set

    private var savedTemplateBaseline: TableTemplateState = initialTemplateState
    private var savedStyleBaseline: TableStyleState = initialStyleState
    private var savedSaveRulesBaseline: MockSaveRulesDraft = saveRulesDraft

    private val undoManager = TableUndoManager<MockEditorSnapshot>()

    var historyRevision by mutableIntStateOf(0)
        private set

    val canUndo: Boolean
        get() = undoManager.canUndo()

    val canRedo: Boolean
        get() = undoManager.canRedo()

    val isDirty: Boolean
        get() =
            draftTemplateState != savedTemplateBaseline ||
                draftStyleState != savedStyleBaseline ||
                saveRulesDraft != savedSaveRulesBaseline

    fun currentSnapshot(): MockEditorSnapshot =
        MockEditorSnapshot(
            templateState = draftTemplateState,
            styleState = draftStyleState,
            includePathInCounterScope = saveRulesDraft.includePathInScope,
            includeFilenameInCounterScope = saveRulesDraft.includeFilenameInScope,
        )

    fun commitTemplateChange(updated: TableTemplateState) {
        if (updated == draftTemplateState) return
        pushCurrentSnapshot()
        draftTemplateState = updated
    }

    fun commitStyleChange(updated: TableStyleState) {
        if (updated == draftStyleState) return
        pushCurrentSnapshot()
        draftStyleState = updated
    }

    fun commitSaveRulesChange(updated: MockSaveRulesDraft) {
        val updatedTemplate = applyMockSaveRulesDraft(draftTemplateState, updated)
        if (updated == saveRulesDraft && updatedTemplate == draftTemplateState) return
        pushCurrentSnapshot()
        saveRulesDraft = updated
        draftTemplateState = updatedTemplate
    }

    fun beginContinuousTemplateChange() {
        pushCurrentSnapshot()
    }

    fun replaceTemplateDraftWithoutHistory(updated: TableTemplateState) {
        draftTemplateState = updated
    }

    fun undo(): Boolean {
        val current = currentSnapshot()
        val restored = undoManager.undo(current)
        if (restored == current) return false
        applyHistorySnapshot(restored)
        return true
    }

    fun redo(): Boolean {
        val current = currentSnapshot()
        val restored = undoManager.redo(current)
        if (restored == current) return false
        applyHistorySnapshot(restored)
        return true
    }

    fun finalTemplateForSave(): TableTemplateState =
        applyMockSaveRulesDraft(draftTemplateState, saveRulesDraft)

    fun markSaved(finalTemplate: TableTemplateState) {
        draftTemplateState = finalTemplate
        savedTemplateBaseline = finalTemplate
        savedStyleBaseline = draftStyleState
        savedSaveRulesBaseline = saveRulesDraft
    }

    private fun pushCurrentSnapshot() {
        undoManager.pushSnapshotBeforeAction(currentSnapshot())
        historyRevision += 1
    }

    private fun applyHistorySnapshot(snapshot: MockEditorSnapshot) {
        draftTemplateState = snapshot.templateState
        draftStyleState = snapshot.styleState
        saveRulesDraft = mockSaveRulesDraftFromTemplate(
            templateState = snapshot.templateState,
            includePathInScope = snapshot.includePathInCounterScope,
            includeFilenameInScope = snapshot.includeFilenameInCounterScope,
        )
        historyRevision += 1
    }
}
