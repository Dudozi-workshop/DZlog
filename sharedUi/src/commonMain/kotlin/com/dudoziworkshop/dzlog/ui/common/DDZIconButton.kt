package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor

enum class DDZIconButtonStyle {
    Neutral,
    Selected,
    Destructive,
}

@Composable
fun DDZIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: DDZIconButtonStyle = DDZIconButtonStyle.Neutral,
) {
    val colors = LocalDDZColor.current

    val containerColor = when (style) {
        DDZIconButtonStyle.Neutral -> Color.Transparent
        DDZIconButtonStyle.Selected -> colors.SelectedSoft
        DDZIconButtonStyle.Destructive -> colors.DestructiveSoft
    }
    val contentColor = when (style) {
        DDZIconButtonStyle.Neutral -> colors.TextPrimary
        DDZIconButtonStyle.Selected -> colors.SelectedDark
        DDZIconButtonStyle.Destructive -> colors.Destructive
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.TextDisabled,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}
