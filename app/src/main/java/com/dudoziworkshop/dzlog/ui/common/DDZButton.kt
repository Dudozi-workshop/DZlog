package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

enum class DDZButtonStyle {
    Primary,
    Secondary,
    Text,
    Destructive,
}

@Composable
fun DDZButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DDZButtonStyle = DDZButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    minHeight: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColorOverride: Color? = null,
    textStyleOverride: TextStyle? = null,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current

    val containerColor = when (style) {
        DDZButtonStyle.Primary -> colors.Primary
        DDZButtonStyle.Secondary -> colors.Surface
        DDZButtonStyle.Text -> Color.Transparent
        DDZButtonStyle.Destructive -> colors.DestructiveSoft
    }
    val contentColor = when (style) {
        DDZButtonStyle.Primary -> colors.OnPrimary
        DDZButtonStyle.Secondary -> colors.TextPrimary
        DDZButtonStyle.Text -> colors.PrimaryDark
        DDZButtonStyle.Destructive -> colors.Destructive
    }
    val border = when (style) {
        DDZButtonStyle.Secondary -> BorderStroke(1.dp, colors.Border)
        else -> null
    }

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        enabled = enabled,
        shape = shape,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColorOverride ?: containerColor,
            contentColor = contentColor,
            disabledContainerColor = when (style) {
                DDZButtonStyle.Text -> Color.Transparent
                else -> colors.SurfaceSoft
            },
            disabledContentColor = colors.TextDisabled,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = textStyleOverride ?: typography.ButtonText,
        )
    }
}
