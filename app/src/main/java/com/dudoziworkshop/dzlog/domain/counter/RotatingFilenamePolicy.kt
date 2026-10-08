package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.naming.resolveRotatingFileNameCell
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterProgressMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell

/** The phrase policy is explicit even when ordinary filename scoping is disabled. */
internal object RotatingFilenamePolicy {
    fun scopeValues(drafts: List<TableEditorSlotDraft?>, cells: List<ResolvedCell>): List<String> =
        drafts.mapNotNull { draft ->
            val cell = when {
                draft?.kind.equals("CELL", true) -> cells.firstOrNull { it.id == draft?.cellId }
                draft?.kind.equals("FORMAT", true) && draft?.formatType.equals("ROTATING_TEXT", true) ->
                    resolveRotatingFileNameCell(cells)
                else -> null
            }?.takeIf { it.type == TableCellDataType.ROTATING_TEXT } ?: return@mapNotNull null
            when (cell.rotatingPhraseSet?.counterProgressMode) {
                RotatingCounterProgressMode.CONTINUOUS -> "continuous:${cell.rotatingPhraseSet.id}"
                else -> "per_phrase:${resolveRotatingCounterStreamIdentity(cell.resolvedText)}"
            }
        }
}
