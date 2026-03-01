package com.dudoziworkshop.dzlog.data.favorites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val uriString: String,
    val relativePath: String,
    val displayName: String,
    val dateAddedSeconds: Long,
    val favoritedAtMillis: Long,
)
