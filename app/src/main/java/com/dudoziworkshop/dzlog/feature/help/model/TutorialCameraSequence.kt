package com.dudoziworkshop.dzlog.feature.help.model

/** A short presentation inside one guide page; never creates extra navigation steps. */
internal data class CameraGuideFrame(val pageId: String, val progress: Float)

internal fun cameraGuideScenes(pageId: String): List<String> = when (pageId) {
    "layout" -> listOf("top", "bottom")
    "saving" -> listOf("shutter", "save-badge", "saving")
    "undo" -> listOf("undo", "warning")
    "zoom" -> listOf("zoom", "pinch")
    "menu" -> listOf("menu")
    "focus" -> listOf("focus", "manual-focus")
    "manual-focus" -> listOf("manual-focus")
    "flash" -> listOf("flash")
    "grid" -> listOf("grid")
    "table" -> listOf("table")
    "assist" -> listOf("assist")
    "select" -> listOf("select-table", "select")
    "size" -> listOf("move", "resize-button", "size", "corner-size")
    "lock" -> listOf("rotation", "lock")
    "quick" -> listOf("quick-button")
    "quick-values" -> listOf("quick-text", "quick-number", "quick-date", "quick-phrase")
    "quick-text" -> listOf("quick-text")
    "quick-number" -> listOf("quick-number")
    "quick-date" -> listOf("quick-date")
    "quick-phrase" -> listOf("quick-phrase")
    "capture-confirmation" -> listOf("capture-confirmation")
    "feedback" -> listOf("sound", "haptic")
    "template" -> listOf("template-name", "template")
    "ratio" -> listOf("entry", "ratio")
    "quality" -> listOf("quality")
    "controls" -> listOf("controls")
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
        "menu" -> "menu"
        "manual-focus" -> "focus"
        "flash" -> "flash"
        "table" -> "table"
        else -> pageId
    }
    "table" -> when {
        pageId == "move" -> "size"
        pageId == "rotation" -> "lock"
        pageId in setOf("quick-text", "quick-number", "quick-date", "quick-phrase") -> pageId
        pageId == "quick-time" -> "quick-date"
        pageId.startsWith("quick-") -> pageId
        else -> pageId
    }
    "settings" -> when (pageId) {
        "entry" -> "ratio"
        "capture-confirmation" -> "capture-confirmation"
        "sound", "haptic" -> "feedback"
        "feedback" -> "feedback"
        "path", "counter" -> "filename"
        else -> pageId
    }
    else -> pageId
}
