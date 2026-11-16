package com.dudoziworkshop.dzlog.data.media

import platform.Foundation.NSUserDefaults

/** Gallery-only preference, independent of workspace backups and Photos metadata. */
class IosGalleryPreferences(private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) {
    fun gridColumns(): Int = defaults.stringForKey(GRID_COLUMNS_KEY)?.toIntOrNull()
        ?.let(SavedGalleryGridPolicy::normalize) ?: SavedGalleryGridPolicy.DEFAULT_COLUMNS

    fun setGridColumns(columns: Int) {
        defaults.setObject(SavedGalleryGridPolicy.normalize(columns).toString(), forKey = GRID_COLUMNS_KEY)
    }

    internal companion object {
        const val GRID_COLUMNS_KEY = "dzlog.gallery.grid-columns.v1"
    }
}
