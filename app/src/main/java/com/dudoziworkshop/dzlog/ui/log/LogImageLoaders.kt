package com.dudoziworkshop.dzlog.ui.log

import com.dudoziworkshop.dzlog.R

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri

@Composable
fun DzThumbnail(uriString: String) {
    val context = LocalContext.current
    var bmp by remember(uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uriString) {
        bmp = runCatching { decodeSampledBitmap(context, uriString, reqSize = 320) }.getOrNull()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bmp != null) {
            Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DzFullImage(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bmp by remember(uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uriString) {
        bmp = runCatching { decodeSampledBitmap(context, uriString, reqSize = 1600) }.getOrNull()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bmp != null) {
            Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun decodeSampledBitmap(
    context: android.content.Context,
    uriString: String,
    reqSize: Int
): android.graphics.Bitmap {
    val uri = uriString.toUri()

    // 1) bounds
    val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, optsBounds)
    }

    // 2) sample
    val (w, h) = optsBounds.outWidth to optsBounds.outHeight
    var inSampleSize = 1
    if (w > reqSize || h > reqSize) {
        val halfW = w / 2
        val halfH = h / 2
        while (halfW / inSampleSize >= reqSize && halfH / inSampleSize >= reqSize) {
            inSampleSize *= 2
        }
    }

    val opts = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        this.inSampleSize = inSampleSize
    }

    context.contentResolver.openInputStream(uri)?.use { input ->
        return BitmapFactory.decodeStream(input, null, opts)
            ?: throw IllegalStateException(context.getString(R.string.error_decode_failed))
    }
    throw IllegalStateException("openInputStream failed")
}
