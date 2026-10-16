package com.dudoziworkshop.dzlog.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/**
 * Four *major* onboarding stages: introduction (two pages), permissions,
 * starter selection, and optional first-camera coaching.
 * Visual progress only; no installation flags or navigation changes.
 */
@Composable
internal fun OnboardingProgressHeader(
    stage: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val safeStage = stage.coerceIn(1, 4)
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "DZlog",
                style = DDZTypography.SettingLabel,
                fontWeight = FontWeight.Bold,
                color = DDZColor.PrimaryDark,
            )
            Text(
                "$label · $safeStage / 4",
                style = DDZTypography.Caption,
                color = DDZColor.TextSecondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .semantics { contentDescription = "전체 4단계 중 ${safeStage}단계: $label" },
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(4) { index ->
                Box(
                    Modifier.weight(1f).height(4.dp)
                        .background(
                            if (index < safeStage) DDZColor.Selected else DDZColor.SurfaceSoft,
                            RoundedCornerShape(50),
                        ),
                )
            }
        }
    }
}
