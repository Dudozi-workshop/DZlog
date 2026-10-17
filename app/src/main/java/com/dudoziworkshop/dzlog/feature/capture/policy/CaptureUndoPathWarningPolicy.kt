package com.dudoziworkshop.dzlog.feature.capture.policy

/**
 * Compare logical capture folders, not physical result/original output subfolders.
 * The camera's displayed save path and the committed request both use these roots.
 */
object CaptureUndoPathWarningPolicy {
    fun logicalFolder(path: String?): String? {
        val trimmed = path?.trim()?.replace('\\', '/')?.trimEnd('/')?.takeIf(String::isNotBlank)
            ?: return null
        val logical = trimmed.removeSuffix("/original").trimEnd('/')
        return logical.takeIf(String::isNotBlank)?.plus("/")
    }

    fun requiresConfirmation(capturedPath: String?, currentPath: String?): Boolean {
        val captured = logicalFolder(capturedPath) ?: return false
        val current = logicalFolder(currentPath) ?: return false
        return captured != current
    }
}
