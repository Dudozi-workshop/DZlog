package com.dudoziworkshop.dzlog.ui.theme

import androidx.compose.ui.graphics.Color

object DDZColor {
    // DZlog UI System v1 · Light Brown Main / Sage Accent
    val Background = Color(0xFFFBF8F3)
    val Surface = Color(0xFFFFFDF9)
    val SurfaceSoft = Color(0xFFEEE4D8)

    // Primary action / brand
    val Primary = Color(0xFFA98F78)
    val PrimaryElevated = Color(0xFF927762)
    val PrimaryDark = Color(0xFF6D5645)
    val OnPrimary = Color.White

    // Selection / active state
    val Selected = Color(0xFF7E9670)
    val SelectedDark = Color(0xFF617757)
    val SelectedSoft = Color(0xFFE3EBDD)

    // Text / icon hierarchy
    val TextPrimary = Color(0xFF332A25)
    val TextSecondary = Color(0xFF6F655D)
    val TextDisabled = Color(0xFFA39A92)
    val IconMuted = Color(0xFF8B827A)

    // Borders
    val Border = Color(0xFFD8CEC4)
    val BorderStrong = Color(0xFFC6BAB0)

    // Destructive
    val Destructive = Color(0xFFD96862)
    val DestructiveSoft = Color(0xFFF8E3E1)

    // Camera / overlay exception tokens
    val OverlayScrim = Color(0xFF2D2926)
    val OverlayText = Color.White

    // Compatibility aliases. Remove gradually as screens migrate to semantic names.
    val Card = SurfaceSoft
    val BrandBrown = PrimaryDark
    val TextMuted = TextSecondary

    val Sage = Selected
    val SageDark = SelectedDark
    val SageLight = SelectedSoft

    val SagePrimary = Selected
    val SageDarkStrong = SelectedDark
    val SageBorder = Color(0xFFB9C7B2)
    val TextStrong = TextPrimary
}
