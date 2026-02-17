package com.example.dzlog.feature.capture.policy

import android.net.Uri
import com.example.dzlog.domain.model.SaveMode

internal object UndoCapturePolicy {

    internal fun consumeUndoTargets(
        stack: MutableList<Uri>,
        saveMode: SaveMode
    ): List<Uri> {
        if (stack.isEmpty()) return emptyList()

        val requestedCount = when (saveMode) {
            SaveMode.BOTH -> 2
            SaveMode.ORIGINAL,
            SaveMode.WATERMARKED -> 1
        }

        val actualCount = minOf(requestedCount, stack.size)
        val startIndex = stack.size - actualCount
        val targets = stack.subList(startIndex, stack.size).toList()
        repeat(actualCount) {
            stack.removeAt(stack.lastIndex)
        }
        return targets
    }

    internal fun restoreUndoTargets(
        stack: MutableList<Uri>,
        targets: List<Uri>
    ) {
        if (targets.isEmpty()) return
        stack.addAll(targets)
    }
}
