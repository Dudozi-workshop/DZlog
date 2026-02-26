package com.dudoziworkshop.dzlog.feature.capture.policy

import android.net.Uri

internal object UndoCapturePolicy {

    internal fun pushCapture(
        stack: MutableList<List<Uri>>,
        captureUris: List<Uri>
    ) {
        val normalized = captureUris
            .asSequence()
            .filter { it != Uri.EMPTY }
            .distinct()
            .toList()
        if (normalized.isEmpty()) return
        stack.add(normalized)
    }

    internal fun consumeLatestCapture(
        stack: MutableList<List<Uri>>
    ): List<Uri> {
        val latest = stack.lastOrNull() ?: return emptyList()
        stack.removeAt(stack.lastIndex)
        return latest
    }

    internal fun restoreCapture(
        stack: MutableList<List<Uri>>,
        captureUris: List<Uri>
    ) {
        if (captureUris.isEmpty()) return
        stack.add(captureUris)
    }
}
