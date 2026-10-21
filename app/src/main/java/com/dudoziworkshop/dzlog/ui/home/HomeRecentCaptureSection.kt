package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.ui.log.parseG1G2FromRelativePath
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
internal fun HomeRecentCaptureSection(
    image: MediaImageItem?,
    timeText: String,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
) {
    val context = LocalContext.current
    val homeImageRequest = androidx.compose.runtime.remember(context, image?.uri) {
        image?.uri?.let { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .size(1200)
                .allowHardware(true)
                .crossfade(false)
                .build()
        }
    }

    HomeRecentCaptureContent(
        hasImage = image != null, displayName = image?.displayName.orEmpty(), timeText = timeText,
        recentLabel = stringResource(com.dudoziworkshop.dzlog.R.string.home_recent_capture),
        emptyTitle = stringResource(com.dudoziworkshop.dzlog.R.string.home_no_recent_capture),
        emptyHint = stringResource(com.dudoziworkshop.dzlog.R.string.home_first_record_hint),
        onOpenAlbum = onOpenAlbum,
        onOpenImage = {
            if (image != null) {
                val (g1, g2) = parseG1G2FromRelativePath(image.relativePath)
                onOpenRecentCaptureGrid(g1, g2, image.relativePath, 0)
            }
        },
        imageContent = {
            AsyncImage(model = homeImageRequest, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        },
    )
}
