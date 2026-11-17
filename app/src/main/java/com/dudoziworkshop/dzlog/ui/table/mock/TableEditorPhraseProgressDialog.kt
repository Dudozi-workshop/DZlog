package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.common.DDZCompactPhraseProgressSheet

/** Table detail and camera quick edit share the same phrase controls. */
@Composable
internal fun TableEditorPhraseProgressDialog(
    cell: TableEditorCellUiModel,
    set: RotatingPhraseSet,
    onProgressChange: (Int, Int) -> Unit,
    onPeriodChange: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    DDZCompactPhraseProgressSheet(
        cellKey = cell.domainCellId ?: cell.id,
        set = set,
        phraseIndex = cell.phraseIndex,
        completedShots = cell.phraseCompletedShots,
        legacyEveryOverride = cell.everyOverride,
        onProgressChange = onProgressChange,
        onPeriodChange = onPeriodChange,
        onDismiss = onDismiss,
    )
}
