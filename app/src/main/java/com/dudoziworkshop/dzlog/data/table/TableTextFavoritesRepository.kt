package com.dudoziworkshop.dzlog.data.table

import android.content.Context
import org.json.JSONArray

/**
 * Table text shortcuts are user-level preferences, independent of a table template.
 * Uses a compact JSON array to avoid database migrations for this small list.
 */
class TableTextFavoritesRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "dzlog_table_text_favorites", Context.MODE_PRIVATE,
    )

    fun load(): List<String> = runCatching {
        val array = JSONArray(preferences.getString(KEY, "[]") ?: "[]")
        (0 until array.length())
            .mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
            .distinct()
            .take(MAX_ITEMS)
    }.getOrDefault(emptyList())

    fun save(items: List<String>) {
        val array = JSONArray()
        items.filter { it.isNotBlank() }.distinct().take(MAX_ITEMS)
            .forEach { array.put(it) }
        preferences.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        const val MAX_ITEMS = 20
        private const val KEY = "items"
    }
}
