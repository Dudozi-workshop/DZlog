package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/**
 * Offline-safe example of a photo with a record table on top.
 * Uses the existing DZlog brand artwork; it is NOT a real camera screenshot.
 * Labels are preview-only and must never be persisted to a starter template.
 */
@Composable
internal fun OnboardingPhotoShowcase(
    labels: List<String>,
    values: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 210.dp,
    showTable: Boolean = true,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(if (compact) 12.dp else 18.dp)
    Box(
        modifier.fillMaxWidth().height(height).clip(shape)
            .background(DDZColor.SelectedSoft),
    ) {
        PhotoShowcaseBackdrop()
        Row(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 9.dp else 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PhotoShowcaseTag(if (compact) "사진 예시" else "DZlog · 구성 예시", compact)
            if (!compact) PhotoShowcaseTag("PHOTO + RECORD", compact)
        }
        if (showTable) {
            PhotoShowcaseRecordTable(
                labels = labels,
                values = values,
                compact = compact,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = if (compact) 9.dp else 16.dp),
            )
        } else {
            Box(
                Modifier.align(Alignment.BottomStart)
                    .padding(if (compact) 9.dp else 14.dp),
            ) {
                PhotoShowcaseTag("표 없이 촬영", compact)
            }
        }
    }
}

@Composable
private fun PhotoShowcaseBackdrop() {
    Box(Modifier.fillMaxSize().background(
        Brush.linearGradient(
            listOf(Color(0xFFD7DECC), Color(0xFF9FB5A4), Color(0xFFC5B5A3)),
        ),
    )) {
        Image(
            painter = painterResource(R.drawable.home_leaf_shadow_c),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.72f,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.17f), Color.Transparent, Color(0xFF35433A).copy(alpha = 0.38f)),
                ),
            ),
        )
    }
}

@Composable
private fun PhotoShowcaseTag(label: String, compact: Boolean) {
    Text(
        text = label,
        color = DDZColor.TextPrimary,
        fontSize = if (compact) 9.sp else 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(DDZColor.Surface.copy(alpha = 0.91f))
            .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun PhotoShowcaseRecordTable(
    labels: List<String>,
    values: List<String>,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val columns = labels.mapIndexed { index, title -> title to values.getOrElse(index) { "" } }
    if (columns.isEmpty()) return
    Column(
        modifier.clip(RoundedCornerShape(if (compact) 7.dp else 10.dp))
            .background(DDZColor.Surface.copy(alpha = 0.96f))
            .border(1.dp, DDZColor.Border, RoundedCornerShape(if (compact) 7.dp else 10.dp)),
    ) {
        PhotoShowcaseTableRow(
            cells = columns.map { it.first },
            background = DDZColor.SurfaceSoft,
            compact = compact,
            strong = true,
        )
        PhotoShowcaseTableRow(
            cells = columns.map { it.second },
            background = DDZColor.Surface,
            compact = compact,
            strong = false,
        )
    }
}

@Composable
private fun PhotoShowcaseTableRow(
    cells: List<String>,
    background: Color,
    compact: Boolean,
    strong: Boolean,
) {
    Row(Modifier.fillMaxWidth().background(background)) {
        cells.forEach { value ->
            Text(
                text = value,
                modifier = Modifier.weight(1f)
                    .padding(horizontal = if (compact) 2.dp else 4.dp, vertical = if (compact) 7.dp else 10.dp),
                color = if (strong) DDZColor.PrimaryDark else DDZColor.TextPrimary,
                fontSize = if (compact) 8.sp else 10.sp,
                fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
