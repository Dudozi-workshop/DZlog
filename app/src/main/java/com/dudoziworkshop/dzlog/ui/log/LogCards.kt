package com.dudoziworkshop.dzlog.ui.log

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.LogGroupSummary
import com.dudoziworkshop.dzlog.ui.theme.Beige100
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogGroupCard(
    summary: LogGroupSummary,
    modifier: Modifier = Modifier,
    titleOverride: String? = null,
    isRootHighlight: Boolean = false,
    isGroupRootHighlight: Boolean = false,
    isSelected: Boolean = false,
) {
    val cardBackground = when {
        isRootHighlight || isGroupRootHighlight -> DDZColor.SageLight
        else -> Beige100
    }

    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .padding(20.dp)
            .alpha(if (isSelected) 0.7f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleOverride ?: summary.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${summary.photoCount}장",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDateKorean(summary.latestDateAddedSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.size(12.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            summary.latestContentUri?.let { uri ->
                DzThumbnail(uri.toString())
            }
        }
    }
}

/**
 * G2에서 사용할 "큰 타일" 카드.
 * - 이미지 비중을 높여 G1 리스트 카드와 UI 차이를 확실히 냄.
 */
@Composable
fun LogGroupTileCard(
    summary: LogGroupSummary,
    isSelected: Boolean,
    isGroupRootHighlight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = if (isGroupRootHighlight) {
            CardDefaults.cardColors(containerColor = DDZColor.SageLight)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                val uri = summary.latestContentUri
                if (uri != null) {
                    DzThumbnail(uri.toString())
                }

                if (isSelected) {
                    // 선택 상태 시 살짝 dim + 체크 표시
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .alpha(0.18f)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.Black)
                            .alpha(0.35f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = summary.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${summary.photoCount}장 · ${formatDateKorean(summary.latestDateAddedSeconds)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private val KOREAN_DATE_FORMAT by lazy {
    SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
}

private fun formatDateKorean(dateAddedSeconds: Long): String {
    if (dateAddedSeconds <= 0L) return "-"
    return KOREAN_DATE_FORMAT.format(Date(dateAddedSeconds * 1_000L))
}

@Composable
fun LogOriginalPhotoEntryCard(
    count: Int,
    latestUriString: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                latestUriString?.let { DzThumbnail(uriString = it) }
            }

            Column {
                Text(
                    text = "원본사진",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$count장",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
