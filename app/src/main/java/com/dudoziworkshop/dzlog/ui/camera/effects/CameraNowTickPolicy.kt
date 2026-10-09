package com.dudoziworkshop.dzlog.ui.camera.effects

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.preview.TickUnit
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate

/** Filename/path FORMAT slots may use time even when the table contains only dates.
 * Minute boundaries also cover local midnight without a UTC-aligned DAY delay.
 */
internal fun cameraNowTickUnit(cells: List<TableCellState>): TickUnit =
    when (decideTickUnitFromTemplate(cells)) {
        TickUnit.SECOND -> TickUnit.SECOND
        TickUnit.MINUTE, TickUnit.DAY -> TickUnit.MINUTE
    }
