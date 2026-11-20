package com.dudoziworkshop.dzlog.ui.camera

internal data class PendingCameraUndoPathWarning(
    val captureUris: List<String>,
    val capturedFolder: String,
    val currentFolder: String,
)
