package com.dudoziworkshop.dzlog.data.media

/** Android's long press enters selection and adds the target; it never toggles an existing selection off. */
object SavedGallerySelectionPolicy {
    fun onLongPress(
        selected: Set<String>,
        target: String,
        selectable: Set<String>,
        locked: Boolean,
    ): Set<String>? {
        if (locked || target !in selectable) return null
        return selected.intersect(selectable) + target
    }
}
