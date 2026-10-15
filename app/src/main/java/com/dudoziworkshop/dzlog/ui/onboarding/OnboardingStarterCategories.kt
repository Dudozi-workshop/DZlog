package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private data class StarterCategoryView(
    val id: String,
    val title: String,
    val detail: String,
    val icon: ImageVector,
)

private val starterCategories = listOf(
    StarterCategoryView("basic", "기본 촬영", "사진과 기록을 가볍게 시작해요.",
        Icons.Default.CameraAlt),
    StarterCategoryView("daily", "일상·취미", "여행 · 반려동물 · 취미 기록",
        Icons.Default.Spa),
    StarterCategoryView("work", "업무·현장", "현장 · 점검 · 연구 기록",
        Icons.Default.Work),
)

/** Compact category selection; changing category never writes a template. */
@Composable
internal fun OnboardingStarterCategories(
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            starterCategories.forEach { item ->
                val selected = item.id == selectedId
                Surface(
                    modifier = Modifier.weight(1f)
                        .heightIn(min = 84.dp)
                        .clickable(role = Role.RadioButton) { onSelect(item.id) }
                        .semantics { this.selected = selected },
                    color = if (selected) DDZColor.SelectedSoft else DDZColor.Surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp, if (selected) DDZColor.Selected else DDZColor.Border,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = if (selected) DDZColor.SelectedDark else DDZColor.PrimaryDark,
                        )
                        Text(
                            item.title,
                            style = DDZTypography.Caption,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = DDZColor.TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
        Text(
            starterCategories.firstOrNull { it.id == selectedId }?.detail.orEmpty(),
            style = DDZTypography.Caption,
            color = DDZColor.TextSecondary,
        )
    }
}
