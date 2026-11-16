package com.dudoziworkshop.dzlog.feature.table.editor

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.dudoziworkshop.dzlog.data.table.TableTextFavoritesRepository

class TableEditorTextFavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TableTextFavoritesRepository(application)

    var favorites by mutableStateOf(repository.load())
        private set

    fun add(text: String) {
        if (text.isBlank()) return
        val next = (listOf(text) + favorites.filterNot { it == text })
            .take(TableTextFavoritesRepository.MAX_ITEMS)
        favorites = next
        repository.save(next)
    }

    fun remove(text: String) {
        val next = favorites.filterNot { it == text }
        favorites = next
        repository.save(next)
    }
}
