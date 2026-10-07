@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZIconButton
import com.dudoziworkshop.dzlog.ui.common.dzScreen
import com.dudoziworkshop.dzlog.ui.common.rememberThreeButtonNavEquivalentBottomPadding
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private fun clampDp(value: Dp, min: Dp, max: Dp): Dp = when {
    value < min -> min
    value > max -> max
    else -> value
}

@Composable
fun HomeScreen(
    tableTemplateState: TableTemplateState,
    activeTemplateName: String = "",
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenSaveSettings: () -> Unit = onOpenTableEditor,
    onOpenAlbum: () -> Unit,
    onOpenRecentCaptureGrid: (g1: String, g2: String, relativePath: String, startIndex: Int) -> Unit,
) {
    val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val settings = uiState.settings
    val nextCounterPreview = uiState.nextCounterPreview
    val latestImage = uiState.latestImage

    androidx.compose.runtime.DisposableEffect(homeViewModel) {
        homeViewModel.activate(tableTemplateState)
        onDispose { homeViewModel.deactivate() }
    }

    LaunchedEffect(tableTemplateState) {
        homeViewModel.updateTemplate(tableTemplateState)
    }

    val defaultCaptureName = stringResource(com.dudoziworkshop.dzlog.R.string.home_default_capture)
    val normalizedTemplateName = activeTemplateName.trim().ifBlank { defaultCaptureName }
    val nextCounterText = remember(nextCounterPreview, settings.counterPadding) {
        val raw = nextCounterPreview.toString()
        if (settings.counterPadding > 0) raw.padStart(settings.counterPadding, '0') else raw
    }

    BoxWithConstraints(modifier = Modifier.dzScreen()) {
        val bottomInset = rememberThreeButtonNavEquivalentBottomPadding()
        val horizontalPad = clampDp(maxWidth * 0.055f, HomeUiSpec.HorizontalPaddingMin, HomeUiSpec.HorizontalPaddingMax)
        val sectionGap = clampDp(maxHeight * 0.020f, HomeUiSpec.SectionGapMin, HomeUiSpec.SectionGapMax)
        val heroTopGap = clampDp(maxHeight * 0.055f, HomeUiSpec.HeroTopGapMin, HomeUiSpec.HeroTopGapMax)

        HomeAmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = horizontalPad,
                    end = horizontalPad,
                    bottom = HomeUiSpec.BottomContentPadding + bottomInset,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeUiSpec.HeaderHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DZlog",
                    style = DDZTypography.HomeMainTitle.copy(letterSpacing = HomeUiSpec.BrandLetterSpacing),
                    color = DDZColor.PrimaryDark,
                    modifier = Modifier.weight(1f),
                )
                DDZIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = stringResource(com.dudoziworkshop.dzlog.R.string.home_settings_content_description),
                    onClick = onOpenSettings,
                )
            }

            Spacer(Modifier.height(heroTopGap))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HomeUiSpec.CurrentCaptureInnerHorizontalPadding),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(com.dudoziworkshop.dzlog.R.string.home_current_capture),
                    style = DDZTypography.Caption.copy(fontWeight = FontWeight.Medium),
                    color = DDZColor.TextSecondary,
                )
                Spacer(Modifier.height(HomeUiSpec.CurrentCaptureVerticalGap))
                Text(
                    text = normalizedTemplateName,
                    style = DDZTypography.ScreenTitle.copy(
                        fontSize = HomeUiSpec.TemplateNameSize,
                        lineHeight = HomeUiSpec.TemplateNameLineHeight,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = DDZColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(HomeUiSpec.CurrentCaptureVerticalGap))
                Text(
                    text = stringResource(com.dudoziworkshop.dzlog.R.string.home_next_capture, nextCounterText),
                    style = DDZTypography.Secondary,
                    color = DDZColor.TextSecondary,
                )
            }

            Spacer(Modifier.height(HomeUiSpec.BeforePrimaryButtonGap))

            DDZButton(
                text = stringResource(com.dudoziworkshop.dzlog.R.string.home_start_capture),
                onClick = onStartCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HomeUiSpec.PrimaryButtonHorizontalInset),
                style = DDZButtonStyle.Primary,
                minHeight = HomeUiSpec.PrimaryButtonHeight,
                shape = RoundedCornerShape(HomeUiSpec.PrimaryButtonRadius),
                textStyleOverride = DDZTypography.ButtonText.copy(
                    fontSize = HomeUiSpec.PrimaryButtonTextSize,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            Spacer(Modifier.height(sectionGap + HomeUiSpec.PrimaryToRecentExtraGap))

            HomeRecentCaptureSection(
                image = latestImage,
                timeText = uiState.latestImageTimeText,
                onOpenAlbum = onOpenAlbum,
                onOpenRecentCaptureGrid = onOpenRecentCaptureGrid,
            )

            Spacer(Modifier.height(sectionGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HomeUiSpec.UtilityButtonGap),
            ) {
                DDZButton(
                    text = stringResource(com.dudoziworkshop.dzlog.R.string.home_table_settings),
                    leadingIcon = Icons.Default.GridView,
                    onClick = onOpenTableEditor,
                    modifier = Modifier.weight(1f),
                    style = DDZButtonStyle.Secondary,
                    minHeight = HomeUiSpec.UtilityButtonHeight,
                    shape = RoundedCornerShape(HomeUiSpec.UtilityButtonRadius),
                    containerColorOverride = DDZColor.SurfaceSoft.copy(alpha = 0.68f),
                    textStyleOverride = DDZTypography.ButtonText.copy(
                        fontSize = HomeUiSpec.UtilityButtonTextSize,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                DDZButton(
                    text = stringResource(com.dudoziworkshop.dzlog.R.string.home_save_settings),
                    leadingIcon = Icons.Default.Folder,
                    onClick = onOpenSaveSettings,
                    modifier = Modifier.weight(1f),
                    style = DDZButtonStyle.Secondary,
                    minHeight = HomeUiSpec.UtilityButtonHeight,
                    shape = RoundedCornerShape(HomeUiSpec.UtilityButtonRadius),
                    containerColorOverride = DDZColor.SurfaceSoft.copy(alpha = 0.68f),
                    textStyleOverride = DDZTypography.ButtonText.copy(
                        fontSize = HomeUiSpec.UtilityButtonTextSize,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
