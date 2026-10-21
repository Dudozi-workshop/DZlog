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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.common.AlbumViewAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
fun HomeRecentCaptureContent(
    hasImage: Boolean,
    displayName: String,
    timeText: String,
    recentLabel: String,
    emptyTitle: String,
    emptyHint: String,
    onOpenAlbum: () -> Unit,
    onOpenImage: () -> Unit,
    albumEnabled: Boolean = true,
    imageContent: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = recentLabel,
            style = DDZTypography.Caption.copy(fontWeight = FontWeight.Medium),
            color = DDZColor.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        AlbumViewAction(onClick = onOpenAlbum, enabled = albumEnabled)
    }

    Spacer(Modifier.height(HomeUiSpec.RecentHeaderToImageGap))

    if (hasImage) {
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
                            onOpenImage()
                        },
                ) {
                    imageContent()
                }
                Spacer(Modifier.height(HomeUiSpec.RecentImageToNameGap))
                Text(
                    text = displayName,
                    style = DDZTypography.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = DDZColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(HomeUiSpec.RecentNameToTimeGap))
                Text(
                    text = timeText,
                    style = DDZTypography.Caption.copy(fontSize = 11.sp),
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
                text = emptyTitle,
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
            )
            Text(
                text = emptyHint,
                style = DDZTypography.Secondary,
                color = DDZColor.TextSecondary,
            )
        }
    }
}
