package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.gallery.GalleryCounterBadgePolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun IosGalleryCounterBadge(
    displayName: String,
    relativePath: String,
    modifier: Modifier = Modifier,
) {
    val label = GalleryCounterBadgePolicy.labelFromDzlogFilename(
        displayName = displayName,
        relativePath = relativePath,
        enabled = true,
    ) ?: return
    Box(
        modifier = modifier.background(Color(0xB8332A25), RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .clearAndSetSemantics { contentDescription = "자동번호 $label" },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = DDZTypography.Caption, color = Color.White, maxLines = 1)
    }
}
