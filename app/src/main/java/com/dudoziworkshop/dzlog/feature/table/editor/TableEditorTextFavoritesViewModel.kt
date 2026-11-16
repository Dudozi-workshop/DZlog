package com.dudoziworkshop.dzlog.feature.table.editor

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.dudoziworkshop.dzlog.data.table.TableTextFavoritesRepository

data class TextFavoriteUndo(
    val before: List<String>,
    val after: List<String>,
    val label: String,
    val serial: Int,
)

class TableEditorTextFavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TableTextFavoritesRepository.shared(application)

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
            before, text, TableTextFavoritesRepository.MAX_ITEMS,
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
        val after = TextFavoriteListPolicy.toggle(favorites, text, TableTextFavoritesRepository.MAX_ITEMS)
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
