package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Neutral skeleton avoids displaying raw MediaStore errors or fake image previews. */
@Composable
internal fun GalleryLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.fillMaxWidth(0.72f).height(20.dp)
                .background(DDZColor.SurfaceSoft, RoundedCornerShape(7.dp))
        )
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(3) {
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .background(DDZColor.SurfaceSoft, RoundedCornerShape(10.dp))
                    )
                }
            }
        }
        repeat(2) {
            Box(
                Modifier.fillMaxWidth().height(54.dp)
                    .background(DDZColor.SurfaceSoft, RoundedCornerShape(11.dp))
            )
        }
    }
}
