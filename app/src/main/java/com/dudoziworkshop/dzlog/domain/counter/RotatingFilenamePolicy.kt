package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell

/** The phrase policy is explicit even when ordinary filename scoping is disabled. */
internal object RotatingFilenamePolicy {
    fun scopeValues(drafts: List<TableEditorSlotDraft?>, cells: List<ResolvedCell>): List<String> =
        drafts.mapNotNull { draft ->
            val cell = when {
                draft?.kind.equals("CELL", true) -> cells.firstOrNull { it.id == draft?.cellId }
                draft?.kind.equals("FORMAT", true) && draft?.formatType.equals("ROTATING_TEXT", true) -> {
                    val rotating = cells.filter { it.type == TableCellDataType.ROTATING_TEXT }
                        .sortedWith(compareBy({ it.raw?.rowIndex ?: 0 }, { it.raw?.colIndex ?: 0 }))
                    rotating.firstOrNull { it.resolvedText.isNotBlank() } ?: rotating.firstOrNull()
                }
                else -> null
            }?.takeIf { it.type == TableCellDataType.ROTATING_TEXT } ?: return@mapNotNull null
            resolveRotatingCounterStreamIdentity(cell.resolvedText, cell.rotatingPhraseSet)
        }
}
