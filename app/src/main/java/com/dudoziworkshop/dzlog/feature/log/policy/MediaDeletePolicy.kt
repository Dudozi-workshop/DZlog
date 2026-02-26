package com.dudoziworkshop.dzlog.feature.log.policy

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest

fun launchMediaDeleteRequest(
    resolver: ContentResolver,
    uris: List<Uri>,
    onLaunchIntentSender: (IntentSenderRequest) -> Unit,
    onLegacyDeleteCompleted: () -> Unit,
) {
    if (uris.isEmpty()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pendingIntent: PendingIntent = MediaStore.createDeleteRequest(resolver, uris)
        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        onLaunchIntentSender(request)
        return
    }

    uris.forEach { uri ->
        runCatching { resolver.delete(uri, null, null) }
    }
    onLegacyDeleteCompleted()
}
