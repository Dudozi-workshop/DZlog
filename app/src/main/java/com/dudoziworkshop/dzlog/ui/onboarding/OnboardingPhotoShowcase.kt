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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Read-only marketing composition; cells are real Compose text, never baked into an image. */
@Composable
internal fun OnboardingPhotoShowcase(
    labels: List<String>,
    values: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 210.dp,
    showTable: Boolean = true,
    compact: Boolean = false,
    showGalleryStrip: Boolean = false,
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 22.dp)
    val description = if (showTable) "사진에 기록표가 겹쳐 보이는 구성 예시"
        else "기록표 없이 촬영한 사진 구성 예시"
    Box(
        modifier.fillMaxWidth().height(height).clip(shape)
            .semantics { contentDescription = description }
            .background(DDZColor.SelectedSoft),
    ) {
        PhotoShowcaseBackdrop(compact)
        if (!compact) {
            MarketingPhone(
                modifier = Modifier.align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 13.dp),
            )
        } else {
            PhotoShowcaseTag(
                label = "사진 미리보기",
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
        }
        if (showGalleryStrip) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(3) { index ->
                    Image(
                        painter = painterResource(
                            when (index) {
                                0 -> R.drawable.onboarding_hero_photo
                                1 -> R.drawable.onboarding_gallery_photo
                                else -> R.drawable.onboarding_starter_photo
                            }
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(7.dp))
                            .background(
                                if (index % 2 == 0) DDZColor.SelectedSoft else DDZColor.SurfaceSoft,
                            ),
                    )
                }
            }
        }
        if (showTable) {
            PhotoShowcaseRecordTable(
                labels = labels,
                values = values,
                compact = compact,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .fillMaxWidth(if (compact) 0.95f else 0.80f)
                    .padding(end = if (compact) 9.dp else 12.dp, bottom = if (compact) 13.dp else 28.dp)
                    .shadow(5.dp, RoundedCornerShape(11.dp)),
            )
        } else {
            PhotoShowcaseTag(
                label = "기록표 없이 자유롭게 촬영",
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun PhotoShowcaseBackdrop(compact: Boolean) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color(0xFFF7EEE4), Color(0xFFD6DFCD), Color(0xFFD0C2AC)),
            ),
        ),
    ) {
        Image(
            painter = painterResource(
                if (compact) R.drawable.onboarding_starter_photo
                else R.drawable.onboarding_hero_photo
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.96f,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color(0xFF756F59).copy(alpha = 0.12f)),
                ),
            ),
        )
    }
}

/** Decorative device frame, deliberately not an interactive camera screenshot. */
@Composable
private fun MarketingPhone(modifier: Modifier = Modifier) {
    Box(
        modifier.width(147.dp).height(204.dp).graphicsLayer(rotationZ = -7f)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(Color(0xFF403D37), RoundedCornerShape(24.dp))
            .padding(7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFCED7C6)),
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_hero_photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.align(Alignment.TopCenter)
                .padding(top = 5.dp)
                .width(37.dp).height(6.dp)
                .background(Color(0xFF403D37), RoundedCornerShape(6.dp)),
        )
        Column(
            modifier = Modifier.align(Alignment.Center)
                .padding(horizontal = 9.dp)
                .background(Color(0xFFF9F6F0).copy(alpha = 0.91f), RoundedCornerShape(7.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "관찰 기록",
                fontSize = 10.sp,
                color = DDZColor.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "날짜 · 2026.10.15",
                fontSize = 8.sp,
                color = DDZColor.TextSecondary,
            )
        }
        Box(
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(22.dp)
                .border(2.dp, Color.White, RoundedCornerShape(50))
                .background(Color(0xFFF9F6F0), RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun PhotoShowcaseTag(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        color = DDZColor.TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.clip(RoundedCornerShape(9.dp))
            .background(DDZColor.Surface.copy(alpha = 0.95f))
            .padding(horizontal = 9.dp, vertical = 6.dp),
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
        modifier.clip(RoundedCornerShape(10.dp))
            .background(DDZColor.Surface)
            .border(1.dp, DDZColor.Border, RoundedCornerShape(10.dp)),
    ) {
        PhotoShowcaseTableRow(columns.map { it.first }, DDZColor.SurfaceSoft, compact, true)
        PhotoShowcaseTableRow(columns.map { it.second }, DDZColor.Surface, compact, false)
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
                    .padding(horizontal = if (compact) 2.dp else 4.dp, vertical = if (compact) 9.dp else 11.dp),
                color = if (strong) DDZColor.PrimaryDark else DDZColor.TextPrimary,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
