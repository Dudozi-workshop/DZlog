package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Two-line settings shared by the Content and Save tabs. */
@Composable
internal fun TableEditorSettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    valueIcon: ImageVector? = null,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (leadingIcon != null) Icon(leadingIcon, contentDescription = null,
                    tint = DDZColor.TextSecondary, modifier = Modifier.size(16.dp))
                Text(label, style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (valueIcon != null) Icon(valueIcon, contentDescription = null,
                    tint = DDZColor.TextSecondary, modifier = Modifier.size(16.dp))
                Text(value, modifier = Modifier.weight(1f), style = DDZTypography.Secondary,
                    color = DDZColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null,
            tint = DDZColor.TextSecondary, modifier = Modifier.size(18.dp))
    }
}
