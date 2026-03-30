package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DDZTypography {
    val ScreenTitle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val HomeMainTitle = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
    val SectionTitle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val CardTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
    val HomeSectionLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val Caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val ButtonText = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val SegmentSmall = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)

    // Compact camera-overlay typography (촬영설정 패널 공용)
    val OverlayTitleCompact = Body.copy(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
    val SectionLabelCompact = Caption.copy(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
}
