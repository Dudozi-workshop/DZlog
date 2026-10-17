package com.dudoziworkshop.dzlog.feature.capture.policy

/**
 * All path sources follow one rule: confirm deletion only if the physical
 * capture's logical folder differs from the current preview folder.
 *
 * A changing filename (including rotating phrases and counters) does not
 * trigger this warning unless it also changes the actual save folder.
 */
object CaptureUndoPathWarningPolicy {
    fun logicalFolder(path: String?): String? {
        val normalized = path?.trim()?.replace('\\', '/')?.trimEnd('/')?.takeIf(String::isNotBlank)
            ?: return null
        // Result+original and original-only modes share a single logical folder.
        val base = normalized.removeSuffix("/original").trimEnd('/')
        return base.takeIf(String::isNotBlank)?.plus("/")
    }

    fun requiresConfirmation(capturedPath: String?, currentPath: String?): Boolean {
        val saved = logicalFolder(capturedPath) ?: return false
        val current = logicalFolder(currentPath) ?: return false
        return saved != current
    }
}
