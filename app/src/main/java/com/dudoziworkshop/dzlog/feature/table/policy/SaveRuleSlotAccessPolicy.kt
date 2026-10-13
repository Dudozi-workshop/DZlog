package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.BASIC_SAVE_RULE_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_UI_MAX_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/**
 * Plan-independent, read-only slot evaluation used by UI and final capture validation.
 * A Pro purchase must be verified by the billing entitlement owner, never by this policy.
 */
enum class SaveRuleTier { FREE, VERIFIED_PRO }

data class SaveRuleSlotEvaluation(
    val fileNameSlotCount: Int,
    val pathSlotCount: Int,
    val fileNameLimit: Int,
    val pathLimit: Int,
) {
    val allowed: Boolean get() =
        fileNameSlotCount <= fileNameLimit && pathSlotCount <= pathLimit

    val excessFileNameCount: Int get() =
        (fileNameSlotCount - fileNameLimit).coerceAtLeast(0)

    val excessPathCount: Int get() =
        (pathSlotCount - pathLimit).coerceAtLeast(0)
}

object SaveRuleSlotAccessPolicy {
    fun evaluate(template: TableTemplateState, tier: SaveRuleTier): SaveRuleSlotEvaluation {
        val fileLimit = if (tier == SaveRuleTier.VERIFIED_PRO) {
            FILE_NAME_SLOT_COUNT
        } else {
            BASIC_SAVE_RULE_SLOT_COUNT
        }
        val pathLimit = if (tier == SaveRuleTier.VERIFIED_PRO) {
            PATH_SLOT_UI_MAX_COUNT
        } else {
            BASIC_SAVE_RULE_SLOT_COUNT
        }
        return SaveRuleSlotEvaluation(
            fileNameSlotCount = activeSlots(template.fileNameSlotDrafts),
            pathSlotCount = activeSlots(template.pathSlotDrafts),
            fileNameLimit = fileLimit,
            pathLimit = pathLimit,
        )
    }

    private fun activeSlots(drafts: List<TableEditorSlotDraft?>): Int =
        drafts.count { draft ->
            draft != null && draft.kind.isNotBlank() && draft.label.isNotBlank()
        }
}
