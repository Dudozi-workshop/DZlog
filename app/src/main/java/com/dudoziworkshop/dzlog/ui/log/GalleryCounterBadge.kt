package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.gallery.GalleryCounterBadgePolicy
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun GalleryCounterBadge(
    photo: MediaImageItem,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = GalleryCounterBadgePolicy.labelFromDzlogFilename(
        displayName = photo.displayName,
        relativePath = photo.relativePath,
        enabled = enabled,
    ) ?: return

    Box(
        modifier = modifier
            .background(Color(0xB8332A25), RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = DDZTypography.Caption, color = Color.White, maxLines = 1)
    }
}
