package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/**
 * Creating a phrase set and assigning it to the editing cell is one operation.
 * The newly created set must always start at its first phrase, regardless of
 * the progress or repeat interval from a previously selected set.
 */
internal object RotatingPhraseSetCreationPolicy {
    data class Result(
        val template: TableTemplateState,
        val selectedSetId: String,
    )

    fun createAndSelect(
        template: TableTemplateState,
        targetCellId: String,
        name: String,
        newSetId: String,
    ): Result? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || newSetId.isBlank()) return null
        if (template.phraseSets.any { it.id == newSetId }) return null
        if (template.cells.none {
                it.cellId == targetCellId && it.dataType == TableCellDataType.ROTATING_TEXT
            }
        ) return null

        val created = RotatingPhraseSet(
            id = newSetId,
            name = trimmedName,
            items = emptyList(),
            defaultEvery = 1,
        )
        return Result(
            template = template.copy(
                phraseSets = template.phraseSets + created,
                cells = template.cells.map { cell ->
                    if (cell.cellId != targetCellId) cell
                    else cell.copy(
                        phraseSetId = created.id,
                        everyOverride = null,
                        phraseIndex = 0,
                        phraseCompletedShots = 0,
                    )
                },
            ),
            selectedSetId = created.id,
        )
    }
}
