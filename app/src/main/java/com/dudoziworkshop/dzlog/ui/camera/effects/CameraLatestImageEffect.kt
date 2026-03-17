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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class LatestImageController(
    val latestImage: MediaImageItem?,
    val reload: () -> Unit,
)

@Composable
internal fun rememberLatestImageController(
    context: Context,
    counterScopeRelativePathKey: String,
    saveMode: SaveMode,
): LatestImageController {
    val scope = rememberCoroutineScope()
    val refreshTick = rememberMediaStoreRefreshTick(context)
    var latestImage by remember { mutableStateOf<MediaImageItem?>(null) }

    fun reload() {
        scope.launch {
            latestImage = withContext(Dispatchers.IO) {
                val reader = DzlogMediaStoreReader(context.contentResolver)
                val baseRelativePath = counterScopeRelativePathKey
                    .substringBefore("|g2=", counterScopeRelativePathKey)
                    .let { if (it.endsWith('/')) it else "$it/" }
                val targetRelativePath = if (saveMode == SaveMode.ORIGINAL_ONLY) {
                    if (baseRelativePath.endsWith("original/")) baseRelativePath else "${baseRelativePath}original/"
                } else {
                    baseRelativePath
                }
                runCatching { reader.loadLatestImageInRelativePath(targetRelativePath) }.getOrNull()
            }
        }
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(refreshTick) { reload() }
    LaunchedEffect(saveMode, counterScopeRelativePathKey) { reload() }

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

