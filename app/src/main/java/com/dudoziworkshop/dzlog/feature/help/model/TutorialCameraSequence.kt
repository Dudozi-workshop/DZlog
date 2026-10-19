package com.dudoziworkshop.dzlog.feature.help.model

/** A short presentation inside one guide page; never creates extra navigation steps. */
internal data class CameraGuideFrame(val pageId: String, val progress: Float)

internal fun cameraGuideScenes(pageId: String): List<String> = when (pageId) {
    "layout" -> listOf("top", "bottom")
    "saving" -> listOf("shutter", "save-badge", "saving")
    "undo" -> listOf("undo", "warning")
    "zoom" -> listOf("zoom", "pinch")
    "manual-focus" -> listOf("menu", "manual-focus")
    "grid" -> listOf("flash", "grid")
    "assist" -> listOf("table", "assist")
    "select" -> listOf("select-table", "select")
    "size" -> listOf("move", "resize-button", "size", "corner-size")
    "lock" -> listOf("rotation", "lock")
    "quick" -> listOf("quick-button", "quick-text", "quick-number", "quick-date", "quick-phrase", "quick-path")
    "template" -> listOf("template-name", "template")
    "ratio" -> listOf("entry", "ratio")
    "quality" -> listOf("quality", "capture-confirmation")
    "controls" -> listOf("feedback", "controls")
    "filename" -> listOf("top", "filename")
    else -> listOf(pageId)
}

internal fun cameraGuideFrame(pageId: String, progress: Float): CameraGuideFrame {
    val scenes = cameraGuideScenes(pageId)
    val position = progress.coerceIn(0f, 1f) * scenes.size
    val index = position.toInt().coerceAtMost(scenes.lastIndex)
    return CameraGuideFrame(scenes[index], (position - index).coerceIn(0f, 1f))
}

internal fun cameraGuideAtBottom(pageId: String): Boolean =
    pageId in setOf("layout", "ratio", "quality", "controls", "filename")

/** Map old saved positions to their merged page while preserving the same session. */
internal fun cameraGuideSavedPage(sessionId: String, pageId: String): String = when (sessionId) {
    "capture" -> when (pageId) {
        "top", "bottom" -> "layout"
        "shutter" -> "saving"
        "result" -> "recent"
        "warning" -> "undo"
        else -> pageId
    }
    "tools" -> when (pageId) {
        "pinch" -> "zoom"
        "menu" -> "manual-focus"
        "flash" -> "grid"
        "table" -> "assist"
        else -> pageId
    }
    "table" -> when {
        pageId == "move" -> "size"
        pageId == "rotation" -> "lock"
        pageId.startsWith("quick-") -> "quick"
        else -> pageId
    }
    "settings" -> when (pageId) {
        "entry" -> "ratio"
        "capture-confirmation" -> "quality"
        "feedback" -> "controls"
        "path", "counter" -> "filename"
        else -> pageId
    }
    else -> pageId
}
