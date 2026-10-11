package com.dudoziworkshop.dzlog.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri

data class RecentBackupShortcut(
    val uri: Uri,
    val displayName: String,
)

/**
 * Remembers only the URI of the last successfully exported file; never makes
 * another copy of its contents. A persisted SAF read grant is mandatory.
 */
class RecentBackupShortcutStore(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val prefs = appContext.getSharedPreferences("dzlog_recent_backup", Context.MODE_PRIVATE)

    fun load(): RecentBackupShortcut? {
        val raw = prefs.getString(KEY_URI, null) ?: return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull()
        val name = prefs.getString(KEY_NAME, null)
        if (uri == null || name.isNullOrBlank() || !hasReadGrant(uri)) {
            clear()
            return null
        }
        return RecentBackupShortcut(uri, name)
    }

    /**
     * Called only after the output stream has closed successfully.
     * A document provider may not allow a durable read grant for CREATE_DOCUMENT.
     * In that case the exported file remains saved; we omit the shortcut.
     */
    fun rememberSuccessfulExport(uri: Uri, displayName: String): Boolean {
        val granted = try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            hasReadGrant(uri)
        } catch (_: SecurityException) {
            false
        }
        if (!granted) {
            clear()
            return false
        }
        val oldUri = prefs.getString(KEY_URI, null)?.let(Uri::parse)
        val saved = prefs.edit()
            .putString(KEY_URI, uri.toString())
            .putString(KEY_NAME, displayName.ifBlank { "DZlog 백업" })
            .commit()
        if (saved && oldUri != null && oldUri != uri) {
            releaseReadGrant(oldUri)
        }
        return saved
    }

    fun clear() {
        val oldUri = prefs.getString(KEY_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }
        prefs.edit().remove(KEY_URI).remove(KEY_NAME).apply()
        if (oldUri != null) releaseReadGrant(oldUri)
    }

    private fun hasReadGrant(uri: Uri): Boolean =
        resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }

    private fun releaseReadGrant(uri: Uri) {
        try {
            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Grant has already expired or was revoked by the document provider.
        }
    }

    private companion object {
        const val KEY_URI = "recent_export_uri"
        const val KEY_NAME = "recent_export_name"
    }
}
