package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

import android.content.Context
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.placement.persistTablePlacementState
import com.dudoziworkshop.dzlog.feature.table.policy.saveTableTemplate
import com.dudoziworkshop.dzlog.feature.table.state.persistTableStyleState

sealed interface TableEditorSaveResult {
    data class Success(
        val savedPlacement: TablePlacementState,
    ) : TableEditorSaveResult

    data class Failure(
        val message: String,
        val isLongToast: Boolean,
    ) : TableEditorSaveResult
}

object TableEditorSaveCoordinator {
    suspend fun persist(
        context: Context,
        templatePayload: TableTemplateState,
        stylePayload: TableStyleState,
        placementPayload: TablePlacementState,
        rollbackTemplate: TableTemplateState,
    ): TableEditorSaveResult {
        val templateSaveResult = saveTableTemplate(context, templatePayload)
        if (templateSaveResult.isFailure) {
            return TableEditorSaveResult.Failure(
                message = "저장 실패: ${templateSaveResult.exceptionOrNull()?.message}",
                isLongToast = false,
            )
        }

        val styleSaveResult = runCatching { persistTableStyleState(context, stylePayload) }
        if (styleSaveResult.isFailure) {
            val rollbackResult = saveTableTemplate(context, rollbackTemplate)
            val rollbackSuffix = if (rollbackResult.isFailure) {
                " (롤백 실패: ${rollbackResult.exceptionOrNull()?.message})"
            } else {
                ""
            }
            return TableEditorSaveResult.Failure(
                message = "서식 저장 실패로 저장을 취소했습니다: ${styleSaveResult.exceptionOrNull()?.message}$rollbackSuffix",
                isLongToast = true,
            )
        }

        val placementSaveResult = runCatching { persistTablePlacementState(context, placementPayload) }
        if (placementSaveResult.isFailure) {
            return TableEditorSaveResult.Failure(
                message = "배치 저장 실패: ${placementSaveResult.exceptionOrNull()?.message}",
                isLongToast = true,
            )
        }

        return TableEditorSaveResult.Success(savedPlacement = placementSaveResult.getOrThrow())
    }
}

