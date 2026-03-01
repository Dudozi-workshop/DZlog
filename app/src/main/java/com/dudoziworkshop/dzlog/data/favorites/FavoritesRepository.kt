package com.dudoziworkshop.dzlog.data.favorites

import android.content.Context
import com.dudoziworkshop.dzlog.data.log.LogDatabase
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FavoritesRepository(
    private val dao: FavoritesDao,
) {
    val favoriteIdsFlow: Flow<Set<Long>> = dao.observeFavoriteIds()
        .map { ids -> ids.toSet() }
        .distinctUntilChanged()

    suspend fun toggleFavorite(item: MediaImageItem) {
        if (dao.exists(item.id)) {
            dao.deleteById(item.id)
        } else {
            dao.upsert(
                FavoriteEntity(
                    mediaId = item.id,
                    uriString = item.uri.toString(),
                    relativePath = item.relativePath,
                    displayName = item.displayName,
                    dateAddedSeconds = item.dateAddedSeconds,
                    favoritedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun removeFavoriteById(mediaId: Long) {
        dao.deleteById(mediaId)
    }
}

object FavoritesProvider {
    @Volatile
    private var INSTANCE: FavoritesRepository? = null

    fun repo(context: Context): FavoritesRepository {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: FavoritesRepository(
                dao = LogDatabase.getInstance(context.applicationContext).favoritesDao()
            ).also { INSTANCE = it }
        }
    }
}
