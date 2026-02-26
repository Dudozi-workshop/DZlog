package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Deprecated("Legacy UI. Use DDZSectionHeader + DDZCard layout instead.")
@Composable
fun DZSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.padding(Spacing.Section)
    ) {
        content()
    }
}
