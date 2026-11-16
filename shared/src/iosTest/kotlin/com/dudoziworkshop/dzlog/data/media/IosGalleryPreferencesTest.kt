package com.dudoziworkshop.dzlog.data.media

import kotlin.test.Test
import kotlin.test.assertEquals
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

class IosGalleryPreferencesTest {
    @Test fun columnsSurviveReopeningWithoutTouchingWorkspace() {
        val suite = "dzlog.gallery.preferences.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            defaults.setObject("workspace sentinel", forKey = "workspace")
            val preferences = IosGalleryPreferences(defaults)
            assertEquals(3, preferences.gridColumns())
            preferences.setGridColumns(5)
            assertEquals(5, IosGalleryPreferences(NSUserDefaults(suiteName = suite)).gridColumns())
            preferences.setGridColumns(1)
            assertEquals(2, preferences.gridColumns())
            preferences.setGridColumns(9)
            assertEquals(5, preferences.gridColumns())
            assertEquals("workspace sentinel", defaults.stringForKey("workspace"))
        } finally { defaults.removePersistentDomainForName(suite) }
    }

    @Test fun corruptOrMissingPreferenceFallsBackWithoutCrashing() {
        val suite = "dzlog.gallery.preferences-corrupt.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            defaults.setObject("invalid", forKey = IosGalleryPreferences.GRID_COLUMNS_KEY)
            assertEquals(3, IosGalleryPreferences(defaults).gridColumns())
            defaults.setObject("-20", forKey = IosGalleryPreferences.GRID_COLUMNS_KEY)
            assertEquals(2, IosGalleryPreferences(defaults).gridColumns())
        } finally { defaults.removePersistentDomainForName(suite) }
    }
}
