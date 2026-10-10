package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Shared read-only filename and physical destination cards for camera and home. */
@Composable
internal fun NextCaptureInfoFields(
    fileName: String,
    relativePaths: List<String>,
) {
    Text("파일명", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Surface(
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            fileName.removeSuffix(".jpg").removeSuffix(".jpeg"),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = DDZTypography.Body,
            color = DDZColor.TextStrong,
        )
    }
    Text("저장 경로", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Surface(
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            relativePaths.forEach { path ->
                Text(path, style = DDZTypography.Body, color = DDZColor.TextStrong)
            }
        }
    }
}
