package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.dudoziworkshop.dzlog.ui.common.splitFileNameForDisplay
import com.dudoziworkshop.dzlog.ui.common.buildSavePathTitle
import com.dudoziworkshop.dzlog.ui.log.parseG1G2FromRelativePath
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CompactPathHeader(
    savePath: String,
    fileName: String,
    fileNameRightLabel: String? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelColumnWidth = remember(textMeasurer, density) {
        val savePathLabelWidth = with(density) {
            textMeasurer.measure(
                text = "저장경로",
                style = DDZTypography.Caption
            ).size.width.toDp()
        }
        val fileNameLabelWidth = with(density) {
            textMeasurer.measure(
                text = "파일명",
                style = DDZTypography.Caption
            ).size.width.toDp()
        }
        maxOf(savePathLabelWidth, fileNameLabelWidth)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Card, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "저장경로",
                modifier = Modifier.width(labelColumnWidth),
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val (g1Part, g2Part) = remember(savePath) {
                    parseG1G2FromRelativePath(savePath)
                }
                val valueStyle = DDZTypography.Body.copy(fontSize = 11.sp, lineHeight = 14.sp)
                val displayPath = buildSavePathTitle(
                    prefix = "Pictures/DZlog",
                    g1 = g1Part,
                    g2 = g2Part.ifBlank { null },
                    sep = "/",
                )
                Text(
                    text = displayPath,
                    modifier = Modifier.fillMaxWidth(),
                    style = valueStyle,
                    color = DDZColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "파일명",
                modifier = Modifier.width(labelColumnWidth),
                style = DDZTypography.Caption,
                color = DDZColor.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val valueStyle = DDZTypography.Body.copy(fontSize = 11.sp, lineHeight = 14.sp)
                val fileNameParts = splitFileNameForDisplay(fileName)
                val counter = fileNameParts.counter
                if (counter.isNullOrBlank()) {
                    Text(
                        text = fileNameParts.prefixText,
                        modifier = Modifier.fillMaxWidth(),
                        style = valueStyle,
                        color = DDZColor.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    val combined = fileNameParts.prefixText + "_" + counter
                    val combinedWidth = with(density) {
                        textMeasurer.measure(
                            text = combined,
                            style = valueStyle,
                        ).size.width.toDp()
                    }

                    if (combinedWidth <= maxWidth) {
                        Text(
                            text = combined,
                            modifier = Modifier.fillMaxWidth(),
                            style = valueStyle,
                            color = DDZColor.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = fileNameParts.prefixText,
                                modifier = Modifier.weight(1f),
                                style = valueStyle,
                                color = DDZColor.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "_" + counter,
                                style = valueStyle,
                                color = DDZColor.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
            fileNameRightLabel?.takeIf { it.isNotBlank() }?.let { label ->
                Text(
                    text = label,
                    style = DDZTypography.Caption,
                    color = DDZColor.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
