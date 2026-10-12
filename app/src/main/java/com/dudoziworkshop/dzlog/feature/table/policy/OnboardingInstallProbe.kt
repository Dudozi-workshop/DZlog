package com.dudoziworkshop.dzlog.feature.table.policy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat

/**
 * Best-effort conservative eligibility check. The caller must NEVER interpret a failed
 * MediaStore query (including missing permission) as "the photo library is empty".
 */
object OnboardingInstallProbe {
    fun isUnupdatedFirstInstall(context: Context): Boolean = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.firstInstallTime > 0L && info.firstInstallTime == info.lastUpdateTime
    }.getOrDefault(false)

    fun verifiedNoDzlogImages(context: Context): Boolean {
        val permission = when {
            Build.VERSION.SDK_INT >= 33 -> Manifest.permission.READ_MEDIA_IMAGES
            else -> Manifest.permission.READ_EXTERNAL_STORAGE
        }
        // Without gallery permission a reinstalled app might not be able to see old media.
        if (ContextCompat.checkSelfPermission(context, permission)
            != PackageManager.PERMISSION_GRANTED) return false

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val column = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH
        } else {
            MediaStore.Images.Media.DATA
        }
        val pattern = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Pictures/DZlog/%"
        } else {
            "%/Pictures/DZlog/%"
        }
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                "$column LIKE ?",
                arrayOf(pattern),
                null,
            )?.use { cursor -> !cursor.moveToFirst() } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
