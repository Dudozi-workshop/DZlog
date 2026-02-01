package com.example.dzlog.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.dzlog.ui.theme.LocalDDZColor
import com.example.dzlog.ui.theme.LocalDDZSpacing
import com.example.dzlog.ui.theme.LocalDDZTypography

@Composable
fun DDZSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    usePrimaryText: Boolean = false
) {
    val colors = LocalDDZColor.current
    val typography = LocalDDZTypography.current
    val spacing = LocalDDZSpacing.current

    Text(
        text = title,
        style = typography.SectionTitle,
        color = if (usePrimaryText) colors.TextPrimary else colors.TextMuted,
        modifier = modifier.padding(bottom = spacing.itemGap),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
