package com.dudoziworkshop.dzlog.data.mediastore

import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore

internal object MediaStoreQueryUtils {

    internal fun queryLatestDisplayNameInRelativePath(
        resolver: ContentResolver,
        relativePath: String
    ): String? {
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media._ID
            )
        } else {
            @Suppress("DEPRECATION")
            arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID
            )
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"
        val selection: String?
        val selectionArgs: Array<String>?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
            selectionArgs = arrayOf(relativePath)
        } else {
            @Suppress("DEPRECATION")
            run {
                selection = "${MediaStore.Images.Media.DATA} LIKE ?"
                selectionArgs = arrayOf("%/$relativePath%")
            }
        }

        return resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            if (nameIdx < 0 || !cursor.moveToFirst()) return@use null
            cursor.getString(nameIdx)
        }
    }
}
