package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object HomeUiSpec {
    val HeaderHeight = 60.dp
    val HorizontalPaddingMin = 18.dp
    val HorizontalPaddingMax = 24.dp
    val HeroTopGapMin = 30.dp
    val HeroTopGapMax = 52.dp
    val MinimumTopGap = 32.dp
    val MinimumBottomGap = 24.dp
    const val CaptureCenterFraction = 0.50f
    val SectionGapMin = 14.dp
    val SectionGapMax = 22.dp

    val CurrentCaptureInnerHorizontalPadding = 14.dp
    val TemplateNameSize = 20.sp
    val TemplateNameLineHeight = 24.sp

    val PrimaryButtonHeight = 50.dp
    val PrimaryButtonRadius = 14.dp
    val PrimaryButtonTextSize = 15.sp
    val PrimaryButtonHorizontalInset = 10.dp

    const val RecentImageWidthFraction = 0.81f
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
    // Single approved top-right shadow. The image and viewport scale independently.
    const val ViewportWidthFraction = 0.53f
    const val ViewportHeightFraction = 0.34f
    const val ImageWidthFraction = 0.85f
    const val ImageHeightToWidthRatio = 4f / 3f
    const val ImageRightOffsetFraction = 0.005f
    const val ImageTopOffsetFraction = -0.01f
    const val ShadowOpacity = 0.84f
    val BlurRadius = 1.5.dp

    // Left and lower edges fade to fully transparent *inside* the viewport.
    // The far-end fade must complete before the hard viewport boundary.
    const val LeftFadeStart = 0.01f
    const val LeftFadeEnd = 0.29f
    const val FarFadeStart = 0.50f
    const val FarFadeEnd = 0.90f

    // Movement stays inside the mask's feathered safety envelope.
    const val MotionSafeXFraction = 0.024f
    const val MotionSafeYFraction = 0.012f
    const val BranchLegMillis = 6_000
    const val SunlightLegMillis = 10_000
    val BranchTravelX = 4.dp
    val BranchTravelY = 1.5.dp
    const val BranchRotation = 0.55f

    val GlowOffsetX = 45.dp
    val GlowOffsetY = (-35).dp
    val GlowWidth = 255.dp
    val GlowHeight = 220.dp
    val GlowBlurRadius = 28.dp
    const val GlowAlpha = 0.035f
}
