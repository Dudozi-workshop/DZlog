package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

internal class TableEditorV2SaveCoordinator {
    var isSaving by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    suspend fun save(
        session: TableEditorV2SessionState,
        onSave: suspend (
            TableTemplateState,
            TableStyleState,
            Boolean,
            Boolean,
            SaveMode,
            Int,
            Int?,
            Boolean,
        ) -> Boolean,
    ): Boolean {
        if (isSaving) return false
        isSaving = true
        errorMessage = null

        val finalTemplate = session.finalTemplateForSave()
        val success = runCatching {
            onSave(
                finalTemplate,
                session.draftStyleState,
                session.saveRulesDraft.includePathInScope,
                session.saveRulesDraft.includeFilenameInScope,
                session.draftSaveMode,
                session.draftCounterPadding,
                session.draftNextCounter,
                session.draftUsesAutoNext,
            )
        }.getOrDefault(false)

        if (success) {
            session.markSaved(finalTemplate)
        } else {
            errorMessage = "저장하지 못했습니다. 변경사항은 유지됩니다."
        }

        isSaving = false
        return success
    }

    fun clearError() {
        errorMessage = null
    }
}
