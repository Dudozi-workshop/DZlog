package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZColor
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.LocalDDZTypography

@Composable
fun DDZSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    usePrimaryText: Boolean = true
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
