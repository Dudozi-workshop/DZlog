package com.example.dzlog.data.mediastore

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

data class SavedMedia(
    val uri: Uri,
    val mediaStoreId: Long,
    val displayName: String,
    val isNameAdjusted: Boolean)

interface MediaStoreSaver {
    fun saveJpeg(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        relativePath: String,
        jpegQuality: Int = 95
    ): SavedMedia

    fun deleteByUri(context: Context, uri: Uri): Boolean

    fun findMaxCounterInFolder(
        context: Context,
        relativePath: String,
        counterDigits: Int,
        fnDelim: String
    ): Int
}
