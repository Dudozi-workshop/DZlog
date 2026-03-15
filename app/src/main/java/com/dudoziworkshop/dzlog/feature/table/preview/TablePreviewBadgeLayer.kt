package com.dudoziworkshop.dzlog.feature.table.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TablePreviewBadgeLayer(
    fileNameBadge: String?,
    savePathBadge: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!fileNameBadge.isNullOrBlank()) {
            BadgeChip(text = fileNameBadge)
        }
        if (!savePathBadge.isNullOrBlank()) {
            BadgeChip(text = savePathBadge)
        }
    }
}

@Composable
private fun BadgeChip(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}
