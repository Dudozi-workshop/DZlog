package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object HomeUiSpec {
    val HeaderHeight = 60.dp
    val HorizontalPaddingMin = 18.dp
    val HorizontalPaddingMax = 24.dp
    val HeroTopGapMin = 30.dp
    val HeroTopGapMax = 52.dp
    val SectionGapMin = 14.dp
    val SectionGapMax = 22.dp

    val CurrentCaptureInnerHorizontalPadding = 14.dp
    val TemplateNameSize = 20.sp
    val TemplateNameLineHeight = 24.sp

    val PrimaryButtonHeight = 50.dp
    val PrimaryButtonRadius = 14.dp
    val PrimaryButtonTextSize = 15.sp
    val PrimaryButtonHorizontalInset = 10.dp

    const val RecentImageWidthFraction = 0.90f
    const val RecentImageAspectRatio = 1.58f
    val RecentImageRadius = 15.dp

    val UtilityButtonHeight = 58.dp
    val UtilityButtonRadius = 13.dp
    val UtilityButtonTextSize = 12.sp
    val UtilityButtonGap = 10.dp

    val BrandLetterSpacing = 1.1.sp
    val CurrentCaptureVerticalGap = 5.dp
    val BeforePrimaryButtonGap = 24.dp
    val PrimaryToRecentExtraGap = 8.dp
    val AlbumActionRadius = 10.dp
    val AlbumActionHorizontalPadding = 8.dp
    val AlbumActionVerticalPadding = 6.dp
    val RecentHeaderToImageGap = 9.dp
    val RecentImageToNameGap = 8.dp
    val RecentNameToTimeGap = 2.dp
    val EmptyStateVerticalPadding = 22.dp
    val EmptyStateGap = 4.dp
    val BorderWidth = 1.dp

    val BottomContentPadding = 18.dp
}

internal object HomeAmbientSpec {
    val CanvasWidth = 250.dp
    val CanvasHeight = 210.dp
    val CanvasOffsetX = 28.dp
    val CanvasOffsetY = (-18).dp
    val BlurRadius = 22.dp

    val StemX = 148.dp
    val StemY = 0.dp
    val StemWidth = 12.dp
    val StemHeight = 182.dp
    const val StemRotation = 20f

    val Leaf1X = 84.dp
    val Leaf1Y = 22.dp
    val Leaf1Width = 92.dp
    val Leaf1Height = 34.dp
    const val Leaf1Rotation = -24f

    val Leaf2X = 143.dp
    val Leaf2Y = 56.dp
    val Leaf2Width = 98.dp
    val Leaf2Height = 36.dp
    const val Leaf2Rotation = 24f

    val Leaf3X = 63.dp
    val Leaf3Y = 91.dp
    val Leaf3Width = 102.dp
    val Leaf3Height = 38.dp
    const val Leaf3Rotation = -18f

    val Leaf4X = 127.dp
    val Leaf4Y = 128.dp
    val Leaf4Width = 92.dp
    val Leaf4Height = 34.dp
    const val Leaf4Rotation = 18f

    // Soft sunlight enters from outside the upper-right edge.
    val GlowOffsetX = 45.dp
    val GlowOffsetY = (-35).dp
    val GlowWidth = 255.dp
    val GlowHeight = 220.dp
    val GlowBlurRadius = 28.dp

    // C-Lite Motion v2: independent leaf cycles prevent mechanical synchrony.
    // Animation durations refer to a ONE-WAY leg (RepeatMode.Reverse).
    const val BranchLegMillis = 7_000
    const val Leaf1LegMillis = 4_500
    const val Leaf2LegMillis = 5_800
    const val Leaf3LegMillis = 6_500
    const val Leaf4LegMillis = 5_100
    const val SunlightLegMillis = 10_000
    val BranchTravelX = 5.dp
    val BranchTravelY = 1.5.dp
    const val BranchRotation = 0.75f
    const val LeafRotation = 1.8f

    // Approved C-Lite shadow strength is deliberately preserved.
    const val ShadowAlpha = 0.12f
    const val StemAlpha = 0.075f
    const val GlowAlpha = 0.035f
}
