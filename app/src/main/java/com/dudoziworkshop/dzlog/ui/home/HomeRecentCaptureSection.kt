package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.ui.log.DzThumbnail
import com.dudoziworkshop.dzlog.ui.log.parseG1G2FromRelativePath
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun HomeRecentCaptureSection(
    image: MediaImageItem?,
    timeText: String,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(com.dudoziworkshop.dzlog.R.string.home_recent_capture),
            style = DDZTypography.SectionTitle,
            color = DDZColor.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(com.dudoziworkshop.dzlog.R.string.home_album_view),
            style = DDZTypography.Secondary.copy(fontWeight = FontWeight.Medium),
            color = DDZColor.PrimaryDark,
            modifier = Modifier
                .clip(RoundedCornerShape(HomeUiSpec.AlbumActionRadius))
                .clickable(onClick = onOpenAlbum)
                .padding(
                    horizontal = HomeUiSpec.AlbumActionHorizontalPadding,
                    vertical = HomeUiSpec.AlbumActionVerticalPadding,
                ),
        )
    }

    Spacer(Modifier.height(HomeUiSpec.RecentHeaderToImageGap))

    if (image != null) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(HomeUiSpec.RecentImageWidthFraction),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(HomeUiSpec.RecentImageAspectRatio)
                        .clip(RoundedCornerShape(HomeUiSpec.RecentImageRadius))
                        .background(DDZColor.Surface)
                        .border(
                            HomeUiSpec.BorderWidth,
                            DDZColor.Border,
                            RoundedCornerShape(HomeUiSpec.RecentImageRadius),
                        )
                        .clickable {
                            val (g1, g2) = parseG1G2FromRelativePath(image.relativePath)
                            onOpenRecentCaptureGrid(g1, g2, image.relativePath, 0)
                        },
                ) {
                    DzThumbnail(image.uri.toString())
                }
                Spacer(Modifier.height(HomeUiSpec.RecentImageToNameGap))
                Text(
                    text = image.displayName,
                    style = DDZTypography.Body.copy(fontWeight = FontWeight.Medium),
                    color = DDZColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(HomeUiSpec.RecentNameToTimeGap))
                Text(
                    text = timeText,
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = HomeUiSpec.EmptyStateVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HomeUiSpec.EmptyStateGap),
        ) {
            Text(
                text = stringResource(com.dudoziworkshop.dzlog.R.string.home_no_recent_capture),
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
            )
            Text(
                text = stringResource(com.dudoziworkshop.dzlog.R.string.home_first_record_hint),
                style = DDZTypography.Secondary,
                color = DDZColor.TextSecondary,
            )
        }
    }
}
