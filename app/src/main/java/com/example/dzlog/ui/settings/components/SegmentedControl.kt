package com.example.dzlog.ui.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Text(
                text = label,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
