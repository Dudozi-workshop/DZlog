package com.dudoziworkshop.dzlog.data.table

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/**
 * App-wide reusable text favorites. Both the table editor and camera quick editor
 * observe the same in-process list; persistence survives process restarts.
 */
class TableTextFavoritesRepository private constructor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "dzlog_table_text_favorites", Context.MODE_PRIVATE,
    )

    var favorites by mutableStateOf(load())
        private set

    private fun load(): List<String> = runCatching {
        val array = JSONArray(preferences.getString(KEY, "[]") ?: "[]")
        (0 until array.length())
            .mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
            .distinct()
            .take(MAX_ITEMS)
    }.getOrDefault(emptyList())

    fun save(items: List<String>) {
        val next = items.filter { it.isNotBlank() }.distinct().take(MAX_ITEMS)
        val array = JSONArray()
        next.forEach { array.put(it) }
        preferences.edit().putString(KEY, array.toString()).apply()
        favorites = next
    }

    companion object {
        const val MAX_ITEMS = 20
        private const val KEY = "items"
        @Volatile private var instance: TableTextFavoritesRepository? = null

        fun shared(context: Context): TableTextFavoritesRepository =
            instance ?: synchronized(this) {
                instance ?: TableTextFavoritesRepository(context.applicationContext)
                    .also { instance = it }
            }
    }
}
