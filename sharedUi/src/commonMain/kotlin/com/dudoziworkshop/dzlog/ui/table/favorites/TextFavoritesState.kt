package com.dudoziworkshop.dzlog.ui.table.favorites

import com.dudoziworkshop.dzlog.feature.table.editor.TextFavoriteListPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class TextFavoriteUndo(
    val before: List<String>,
    val after: List<String>,
    val label: String,
    val serial: Int,
)

internal const val MAX_TEXT_FAVORITES = 20
internal val LocalTextFavorites = androidx.compose.runtime.staticCompositionLocalOf<TextFavoritesState?> { null }

internal interface TextFavoritesStore {
    val favorites: List<String>
    fun save(items: List<String>)
}

internal class TextFavoritesState(private val repository: TextFavoritesStore) {
    val favorites: List<String> get() = repository.favorites

    var lastChange by mutableStateOf<TextFavoriteUndo?>(null)
        private set

    var limitReached by mutableStateOf(false)
        private set

    var managementOpen by mutableStateOf(false)
        private set

    fun openManagement() { managementOpen = true }
    fun closeManagement() { managementOpen = false }

    private var serial = 0

    /** Returns false when a new favorite cannot be added at the configured limit. */
    fun toggle(text: String): Boolean {
        if (text.isBlank()) return false
        val before = favorites
        val stored = text in before
        val after = TextFavoriteListPolicy.toggle(
            before, text, MAX_TEXT_FAVORITES,
        ) ?: run {
            if (text.isNotBlank() && !stored) limitReached = true
            return false
        }
        repository.save(after)
        lastChange = TextFavoriteUndo(
            before, after,
            if (stored) "즐겨찾기를 해제했습니다." else "즐겨찾기에 등록했습니다.",
            ++serial,
        )
        return true
    }

    fun undo() {
        val change = lastChange ?: return
        if (favorites == change.after) repository.save(change.before)
        lastChange = null
    }

    fun clearUndo(serial: Int) {
        if (lastChange?.serial == serial) lastChange = null
    }

    fun dismissLimit() { limitReached = false }

    fun remove(text: String) {
        val after = TextFavoriteListPolicy.toggle(favorites, text, MAX_TEXT_FAVORITES)
            ?: return
        if (text !in favorites) return
        repository.save(after)
        lastChange = null
    }

    fun rename(old: String, next: String): Boolean {
        val updated = TextFavoriteListPolicy.rename(favorites, old, next) ?: return false
        repository.save(updated)
        lastChange = null
        return true
    }

    fun move(text: String, direction: Int) {
        val next = TextFavoriteListPolicy.move(favorites, text, direction)
        if (next == favorites) return
        repository.save(next)
        lastChange = null
    }
}
