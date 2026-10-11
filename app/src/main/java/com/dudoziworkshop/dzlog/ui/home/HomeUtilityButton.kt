package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun HomeUtilityButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = HomeUiSpec.UtilityButtonHeight),
        shape = RoundedCornerShape(HomeUiSpec.UtilityButtonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = DDZColor.SurfaceSoft.copy(alpha = 0.68f),
            contentColor = DDZColor.TextPrimary,
        ),
        border = BorderStroke(HomeUiSpec.BorderWidth, DDZColor.Border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = DDZTypography.ButtonText.copy(
                    fontSize = HomeUiSpec.UtilityButtonTextSize,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
