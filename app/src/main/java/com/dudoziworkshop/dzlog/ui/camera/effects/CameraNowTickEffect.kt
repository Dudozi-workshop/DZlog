package com.dudoziworkshop.dzlog.ui.camera.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import kotlinx.coroutines.delay
import java.util.Date

@Composable
internal fun CameraNowTickEffect(
    lifecycleOwner: LifecycleOwner,
    cells: List<TableCellState>,
    onNowChange: (Date) -> Unit,
) {
    LaunchedEffect(cells, lifecycleOwner) {
        val unit = decideTickUnitFromTemplate(cells)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val delayMs = computeNextDelayMillis(unit)
                delay(delayMs)
                onNowChange(Date())
            }
        }
    }
}

