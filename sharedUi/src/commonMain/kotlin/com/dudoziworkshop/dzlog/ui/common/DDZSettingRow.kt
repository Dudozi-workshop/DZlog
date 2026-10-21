package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@Composable
fun DDZSettingRow(
    label: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (leadingIcon != null) Icon(leadingIcon, contentDescription = null,
                    tint = colors.TextSecondary, modifier = Modifier.size(18.dp))
                Text(text = label, style = typography.SettingLabel, color = colors.TextPrimary)
            }
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = typography.Secondary,
                    color = colors.TextSecondary,
                )
            }
        }

        when {
            trailingContent != null -> trailingContent()
            onClick != null -> Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.TextSecondary,
            )
        }
    }
}
