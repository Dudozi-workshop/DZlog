package com.dudoziworkshop.dzlog.feature.log.policy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal enum class GalleryMediaAccess { FULL, SELECTED_ONLY, NOT_GRANTED }

internal object GalleryMediaAccessPolicy {
    fun state(context: Context): GalleryMediaAccess {
        fun has(permission: String) =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        return when {
            Build.VERSION.SDK_INT >= 33 &&
                has(Manifest.permission.READ_MEDIA_IMAGES) -> GalleryMediaAccess.FULL
            Build.VERSION.SDK_INT >= 34 &&
                has(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ->
                GalleryMediaAccess.SELECTED_ONLY
            Build.VERSION.SDK_INT < 33 &&
                has(Manifest.permission.READ_EXTERNAL_STORAGE) -> GalleryMediaAccess.FULL
            else -> GalleryMediaAccess.NOT_GRANTED
        }
    }

    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
