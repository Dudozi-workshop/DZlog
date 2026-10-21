package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
fun TableEditorTemplateSelector(name: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "템플릿 교체", onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("템플릿", style = DDZTypography.Caption, color = DDZColor.TextMuted)
        Text(name, modifier = Modifier.weight(1f), style = DDZTypography.Body,
            fontWeight = FontWeight.SemiBold, color = DDZColor.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (enabled) Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
            modifier = Modifier.size(18.dp), tint = DDZColor.Primary)
    }
}
