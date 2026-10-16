package com.dudoziworkshop.dzlog.ui.camera.effects

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class LatestImageController(
    val latestImage: MediaImageItem?,
    val reload: () -> Unit,
)

@Composable
internal fun rememberLatestImageController(
    context: Context,
    sessionCaptureStack: List<List<android.net.Uri>>,
): LatestImageController {
    val refreshTick = rememberMediaStoreRefreshTick(context)
    var latestImage by remember { mutableStateOf<MediaImageItem?>(null) }

    var reloadTick by remember { mutableIntStateOf(0) }
    fun reload() { reloadTick += 1 }

    // Reading the observable stack makes a new committed shutter trigger a
    // refresh even if its save folder differs from the next capture's folder.
    val savedGroups = sessionCaptureStack.map { group -> group.toList() }

    // Do not clear an existing thumbnail merely because the save path or
    // save mode changed. A cancellable lookup replaces it after reconciliation.
    LaunchedEffect(context, refreshTick, savedGroups, reloadTick) {
        latestImage = withContext(Dispatchers.IO) {
            val reader = DzlogMediaStoreReader(context.contentResolver)
            runCatching {
                if (savedGroups.isNotEmpty()) {
                    reader.loadLatestImageInSession(savedGroups)
                } else {
                    // Outside a continuous session, retain a useful global
                    // recent-photo thumbnail instead of a blank new-folder tile.
                    reader.loadLatestImage()
                }
            }.getOrNull()
        }
    }

    return LatestImageController(
        latestImage = latestImage,
        reload = ::reload
    )
}

@Composable
private fun rememberMediaStoreRefreshTick(context: Context): Int {
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshTick += 1
            }
        }

        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    return refreshTick
}

