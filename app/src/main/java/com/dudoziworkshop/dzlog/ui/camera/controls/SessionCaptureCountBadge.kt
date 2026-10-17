package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Non-interactive shot counter matching the save-status badge footprint. */
@Composable
internal fun SessionCaptureCountBadge(
    completedCount: Int,
    modifier: Modifier = Modifier,
) {
    if (completedCount <= 0) return
    Box(modifier.width(48.dp).height(48.dp), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .width(46.dp).height(24.dp)
                .background(DDZColor.SelectedSoft.copy(alpha = 0.88f), RoundedCornerShape(999.dp))
                .semantics(mergeDescendants = true) {
                    contentDescription = "이번 촬영 ${completedCount}회"
                }
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = DDZColor.SelectionEmphasis,
                modifier = Modifier.width(13.dp).height(13.dp),
            )
            Text(
                text = completedCount.toString(),
                color = DDZColor.SelectionEmphasis,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
