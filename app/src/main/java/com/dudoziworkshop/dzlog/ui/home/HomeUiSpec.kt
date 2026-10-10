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
    // One approved botanical shadow. Keep the original PNG untouched.
    // The window is screen-relative and stays independent from image size.
    const val VisibleShadowWidthFraction = 0.43f
    const val VisibleShadowHeightFraction = 0.38f
    const val ImageToViewportWidthRatio = 1.52f
    const val ImageOffsetXFraction = 0.015f
    const val ImageOffsetYFraction = -0.05f
    // Current installed asset is 220x185; do not stretch its aspect ratio.
    const val ShadowHeightToWidthRatio = 185f / 220f
    const val ShadowOpacity = 0.90f
    const val LeftFadeStart = 0.0f
    const val LeftFadeEnd = 0.42f
    const val FarFadeStart = 0.55f
    const val FarFadeEnd = 1.0f
    val BlurRadius = 1.5.dp

    val GlowOffsetX = 45.dp
    val GlowOffsetY = (-35).dp
    val GlowWidth = 255.dp
    val GlowHeight = 220.dp
    val GlowBlurRadius = 28.dp
    const val GlowAlpha = 0.035f

    // Preserve slow whole-image movement for v1; per-leaf motion comes later.
    const val BranchLegMillis = 6_000
    const val SunlightLegMillis = 10_000
    val BranchTravelX = 6.dp
    val BranchTravelY = 1.5.dp
    const val BranchRotation = 0.85f
}
