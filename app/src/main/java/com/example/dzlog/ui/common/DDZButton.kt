package com.example.dzlog.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.ui.theme.LocalDDZColor
import com.example.dzlog.ui.theme.LocalDDZTypography
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

enum class DDZButtonStyle {
    Primary,
    Secondary
}

@Composable
fun DDZButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DDZButtonStyle = DDZButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current

    val containerColor = when (style) {
        DDZButtonStyle.Primary -> colors.Primary
        DDZButtonStyle.Secondary -> colors.Surface
    }
    val contentColor = when (style) {
        DDZButtonStyle.Primary -> colors.Surface
        DDZButtonStyle.Secondary -> colors.TextPrimary
    }
    val border = when (style) {
        DDZButtonStyle.Primary -> null
        DDZButtonStyle.Secondary -> BorderStroke(1.dp, colors.Border)
    }

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = typography.ButtonText
        )
    }
}
