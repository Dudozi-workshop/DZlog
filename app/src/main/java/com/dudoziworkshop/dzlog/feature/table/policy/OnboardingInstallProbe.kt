package com.dudoziworkshop.dzlog.feature.table.policy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat

/** Conservative signals for startup routing; no settings or photos are modified. */
object OnboardingInstallProbe {
    fun isUnupdatedFirstInstall(context: Context): Boolean = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.firstInstallTime > 0L && info.firstInstallTime == info.lastUpdateTime
    }.getOrDefault(false)

    /**
     * true: DZlog pictures were found. false: query completed without finding any.
     * null: library unavailable / permission absent, never assume emptiness.
     */
    fun existingDzlogImagesOrNull(context: Context): Boolean? {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, permission) !=
            PackageManager.PERMISSION_GRANTED) return null
        val column = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH
        } else MediaStore.Images.Media.DATA
        val pattern = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Pictures/DZlog/%"
        } else "%/Pictures/DZlog/%"
        return try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                "$column LIKE ?",
                arrayOf(pattern),
                null,
            )?.use { it.moveToFirst() }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
