package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DDZTypography {
    val ScreenTitle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val HomeMainTitle = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)

    val SectionTitle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val SettingLabel = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val Secondary = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
    val Caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)

    val ButtonText = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val SegmentSmall = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)

    // Compatibility aliases while existing screens migrate to the v1 hierarchy.
    val CardTitle = SectionTitle
    val HomeSectionLabel = SettingLabel

    // Compact camera-overlay typography. Overlay is an explicit dark-surface exception.
    val OverlayTitleCompact = Body.copy(
        fontSize = 14.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val SectionLabelCompact = Caption.copy(
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
    )
}
