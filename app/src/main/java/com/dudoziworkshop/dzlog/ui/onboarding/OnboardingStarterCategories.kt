package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

private data class StarterCategoryView(
    val id: String,
    val title: String,
    val icon: ImageVector,
)

private val starterCategories = listOf(
    StarterCategoryView("basic", "기본 촬영", Icons.Default.CameraAlt),
    StarterCategoryView("daily", "일상·취미", Icons.Default.Spa),
    StarterCategoryView("work", "업무·현장", Icons.Default.Work),
)

/** Three approachable cards. Changing selection does not persist any template. */
@Composable
internal fun OnboardingStarterCategories(
    selectedId: String,
    onSelect: (String) -> Unit,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        starterCategories.forEach { item ->
            val selected = selectedId == item.id
            Surface(
                modifier = Modifier.weight(1f)
                    .heightIn(min = if (compact) 81.dp else 92.dp)
                    .clickable(role = Role.RadioButton) { onSelect(item.id) }
                    .semantics { this.selected = selected },
                color = if (selected) DDZColor.SelectedSoft else DDZColor.Surface,
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) DDZColor.Selected else DDZColor.Border.copy(alpha = 0.65f),
                ),
                shadowElevation = if (selected) 2.dp else 1.dp,
            ) {
                Box {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = if (compact) 8.dp else 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(if (compact) 32.dp else 36.dp)
                                .background(
                                    if (selected) DDZColor.Selected.copy(alpha = 0.14f)
                                    else DDZColor.AmbientWarm, CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                item.icon, contentDescription = null,
                                tint = if (selected) DDZColor.SelectedDark else DDZColor.PrimaryDark,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            item.title, fontSize = if (compact) 12.sp else 13.sp,
                            lineHeight = 17.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = DDZColor.TextPrimary,
                            maxLines = 2,
                        )
                    }
                    if (selected) {
                        Icon(
                            Icons.Default.CheckCircle, contentDescription = null,
                            tint = DDZColor.SelectedDark,
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
