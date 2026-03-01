package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun DzThumbnail(uriString: String) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(uriString)
        .crossfade(false)
        .allowHardware(true)
        .size(128)
        .build()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun DzFullImage(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(uriString)
        .crossfade(false)
        .allowHardware(true)
        .size(2048)
        .build()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
