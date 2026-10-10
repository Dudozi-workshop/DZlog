package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableUndoManager
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

internal data class MockEditorSnapshot(
    val templateState: TableTemplateState,
    val styleState: TableStyleState,
    val includePathInCounterScope: Boolean,
    val includeFilenameInCounterScope: Boolean,
    val saveMode: SaveMode,
    val counterPadding: Int,
    val nextCounter: Int?,
    val usesAutoNext: Boolean,
)

internal class TableEditorV2SessionState(
    initialTemplateState: TableTemplateState,
    initialStyleState: TableStyleState,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    initialSaveMode: SaveMode = SaveMode.BOTH,
    initialCounterPadding: Int = 0,
) {
    var draftTemplateState by mutableStateOf(initialTemplateState)
        private set

    var draftStyleState by mutableStateOf(initialStyleState)
        private set

    var draftSaveMode by mutableStateOf(initialSaveMode)
        private set

    var draftCounterPadding by mutableIntStateOf(initialCounterPadding.coerceIn(0, 4))
        private set

    var draftNextCounter by mutableStateOf<Int?>(null)
        private set

    var draftUsesAutoNext by mutableStateOf(true)
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
    private var savedSaveModeBaseline: SaveMode = draftSaveMode
    // Keep the persisted baseline so legacy 5/6-digit normalization is saved only by the user.
    private var savedCounterPaddingBaseline: Int = initialCounterPadding.coerceIn(0, 6)
    private var savedNextCounterBaseline: Int? = draftNextCounter
    private var savedUsesAutoNextBaseline: Boolean = draftUsesAutoNext
    private var counterBaselineInitialized: Boolean = false

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
                saveRulesDraft != savedSaveRulesBaseline ||
                draftSaveMode != savedSaveModeBaseline ||
                draftCounterPadding != savedCounterPaddingBaseline ||
                draftNextCounter != savedNextCounterBaseline ||
                draftUsesAutoNext != savedUsesAutoNextBaseline

    fun currentSnapshot(): MockEditorSnapshot =
        MockEditorSnapshot(
            templateState = draftTemplateState,
            styleState = draftStyleState,
            includePathInCounterScope = saveRulesDraft.includePathInScope,
            includeFilenameInCounterScope = saveRulesDraft.includeFilenameInScope,
            saveMode = draftSaveMode,
            counterPadding = draftCounterPadding,
            nextCounter = draftNextCounter,
            usesAutoNext = draftUsesAutoNext,
        )

    fun commitTemplateChange(updated: TableTemplateState) {
        if (updated == draftTemplateState) return
        pushCurrentSnapshot()
        draftTemplateState = updated
        saveRulesDraft = mockSaveRulesDraftFromTemplate(
            templateState = updated,
            includePathInScope = saveRulesDraft.includePathInScope,
            includeFilenameInScope = saveRulesDraft.includeFilenameInScope,
        )
        invalidateCounterDraft()
    }

    fun commitStyleChange(updated: TableStyleState) {
        if (updated == draftStyleState) return
        pushCurrentSnapshot()
        draftStyleState = updated
    }

    fun commitSaveModeChange(updated: SaveMode) {
        if (updated == draftSaveMode) return
        pushCurrentSnapshot()
        draftSaveMode = updated
        invalidateCounterDraft()
    }

    fun initializeCounterState(next: Int, usesAutoNext: Boolean) {
        if (draftNextCounter != null) return
        val normalized = next.coerceAtLeast(1)
        draftNextCounter = normalized
        draftUsesAutoNext = usesAutoNext
        if (!counterBaselineInitialized) {
            savedNextCounterBaseline = normalized
            savedUsesAutoNextBaseline = usesAutoNext
            counterBaselineInitialized = true
        }
    }

    fun commitNextCounterChange(updated: Int, usesAutoNext: Boolean) {
        val normalized = updated.coerceAtLeast(1)
        if (draftNextCounter == normalized && draftUsesAutoNext == usesAutoNext) return
        pushCurrentSnapshot()
        draftNextCounter = normalized
        draftUsesAutoNext = usesAutoNext
    }

    fun commitCounterPaddingChange(updated: Int) {
        val normalized = updated.coerceIn(1, 4)
        if (normalized == draftCounterPadding) return
        pushCurrentSnapshot()
        draftCounterPadding = normalized
    }

    fun commitSaveRulesChange(updated: MockSaveRulesDraft) {
        val updatedTemplate = applyMockSaveRulesDraft(draftTemplateState, updated)
        if (updated == saveRulesDraft && updatedTemplate == draftTemplateState) return
        pushCurrentSnapshot()
        saveRulesDraft = updated
        draftTemplateState = updatedTemplate
        invalidateCounterDraft()
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

    fun markSaved(finalTemplate: TableTemplateState, savedSnapshot: MockEditorSnapshot = currentSnapshot()) {
        val savedRules = mockSaveRulesDraftFromTemplate(
            templateState = finalTemplate,
            includePathInScope = savedSnapshot.includePathInCounterScope,
            includeFilenameInScope = savedSnapshot.includeFilenameInCounterScope,
        )
        // Saving may suspend while the user continues editing. Only normalize an unchanged draft.
        if (currentSnapshot() == savedSnapshot) {
            draftTemplateState = finalTemplate
            saveRulesDraft = savedRules
        }
        savedTemplateBaseline = finalTemplate
        savedStyleBaseline = savedSnapshot.styleState
        savedSaveRulesBaseline = savedRules
        savedSaveModeBaseline = savedSnapshot.saveMode
        savedCounterPaddingBaseline = savedSnapshot.counterPadding
        savedNextCounterBaseline = savedSnapshot.nextCounter
        savedUsesAutoNextBaseline = savedSnapshot.usesAutoNext
        counterBaselineInitialized = savedSnapshot.nextCounter != null
    }

    private fun invalidateCounterDraft() {
        draftNextCounter = null
        draftUsesAutoNext = true
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
        draftSaveMode = snapshot.saveMode
        draftCounterPadding = snapshot.counterPadding
        draftNextCounter = snapshot.nextCounter
        draftUsesAutoNext = snapshot.usesAutoNext
        historyRevision += 1
    }
}

