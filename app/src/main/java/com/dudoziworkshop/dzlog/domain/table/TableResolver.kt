package com.dudoziworkshop.dzlog.domain.table

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.time.asCaptureTime
import java.util.Date
import java.util.Locale

/** Existing Android API; cell, counter and date policies are shared. */
class TableResolver {
    data class Config(
        val counterDigits: Int,
        val dateFormat: String,
        val timeFormat: String,
        val locale: Locale = Locale.getDefault(),
    )

    fun plan(
        cells: List<TableCellState>,
        captureNow: Date,
        config: Config,
        counterSeedOverride: Int? = null,
        selectedPhraseTextByCellId: Map<String, String> = emptyMap(),
        phraseSets: List<RotatingPhraseSet> = emptyList(),
    ): ResolvePlan = TableValueResolver().plan(
        cells, captureNow.asCaptureTime(config.locale),
        TableValueResolver.Config(config.counterDigits, config.dateFormat, config.timeFormat),
        counterSeedOverride, selectedPhraseTextByCellId, phraseSets,
    )
}
