package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem

/** Viewer adapter for the same deletion flow used by selected grid photos. */
@Composable
internal fun GalleryViewerDeleteDialog(
    item: MediaImageItem,
    reader: DzlogMediaStoreReader,
    onVerified: (Set<Long>) -> Unit,
    onClose: () -> Unit,
) {
    GallerySelectedPhotoDeleteDialog(
        selected = listOf(item),
        reader = reader,
        onVerified = onVerified,
        onClose = onClose,
    )
}
