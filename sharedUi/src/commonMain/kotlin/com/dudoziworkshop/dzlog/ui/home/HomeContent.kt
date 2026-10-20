@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZIconButton
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

import androidx.compose.ui.semantics.Role

private fun clampDp(value: Dp, min: Dp, max: Dp): Dp = when {
    value < min -> min
    value > max -> max
    else -> value
}

/** Localized presentation supplied by each platform; no storage or lifecycle here. */
data class HomeLabels(
    val settings: String, val currentTemplate: String, val nextCounter: String,
    val startCapture: String, val tableSettings: String, val saveInfo: String,
)

@Composable
fun HomeContent(
    templateName: String,
    labels: HomeLabels,
    onManageTemplates: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onOpenTableEditor: () -> Unit,
    onOpenSaveInfo: () -> Unit,
    modifier: Modifier,
    bottomInset: Dp,
    captureEnabled: Boolean = true,
    settingsEnabled: Boolean = true,
    editingEnabled: Boolean = true,
    saveInfoEnabled: Boolean = true,
    background: @Composable () -> Unit,
    recentCapture: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val availableScreenHeight = maxHeight
        val horizontalPad = clampDp(maxWidth * 0.055f, HomeUiSpec.HorizontalPaddingMin, HomeUiSpec.HorizontalPaddingMax)
        val sectionGap = clampDp(maxHeight * 0.020f, HomeUiSpec.SectionGapMin, HomeUiSpec.SectionGapMax)
        val heroTopGap = clampDp(maxHeight * 0.048f, HomeUiSpec.HeroTopGapMin, HomeUiSpec.HeroTopGapMax)

        background()

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
            HomeThreeZoneLayout(
                minViewportHeight = (availableScreenHeight - HomeUiSpec.BottomContentPadding - bottomInset).coerceAtLeast(0.dp),
                minTopGap = HomeUiSpec.MinimumTopGap,
                minBottomGap = HomeUiSpec.MinimumBottomGap,
                targetActionCenterFraction = HomeUiSpec.CaptureCenterFraction,
                modifier = Modifier.fillMaxWidth(),
                top = {
                    Column {

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
                            contentDescription = labels.settings,
                            onClick = onOpenSettings,
                            enabled = settingsEnabled,
                        )
                    }

                    Spacer(Modifier.height(heroTopGap))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClickLabel = "템플릿 선택") { onManageTemplates() }
                            .padding(horizontal = HomeUiSpec.CurrentCaptureInnerHorizontalPadding),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = labels.currentTemplate,
                            style = DDZTypography.Caption.copy(fontWeight = FontWeight.Medium),
                            color = DDZColor.TextSecondary,
                        )
                        Spacer(Modifier.height(HomeUiSpec.CurrentCaptureVerticalGap))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = templateName,
                                style = DDZTypography.ScreenTitle.copy(
                                    fontSize = HomeUiSpec.TemplateNameSize,
                                    lineHeight = HomeUiSpec.TemplateNameLineHeight,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = DDZColor.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "템플릿 선택",
                                tint = DDZColor.TextSecondary,
                            )
                        }
                        Spacer(Modifier.height(HomeUiSpec.CurrentCaptureVerticalGap))
                        Text(
                            text = labels.nextCounter,
                            style = DDZTypography.Secondary,
                            color = DDZColor.TextSecondary,
                        )
                    }
                    }
                },
                action = {
                    DDZButton(
                        text = labels.startCapture,
                        onClick = onStartCamera,
                        enabled = captureEnabled,
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
                },
                bottom = {
                    Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HomeUiSpec.PrimaryButtonHorizontalInset),
                    ) {
                        recentCapture()
                    }

                    Spacer(Modifier.height(sectionGap))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HomeUiSpec.PrimaryButtonHorizontalInset),
                        horizontalArrangement = Arrangement.spacedBy(HomeUiSpec.UtilityButtonGap),
                    ) {
                        HomeUtilityButton(
                            label = labels.tableSettings,
                            icon = Icons.Default.GridView,
                            onClick = onOpenTableEditor,
                            enabled = editingEnabled,
                            modifier = Modifier.weight(1f),
                        )
                        HomeUtilityButton(
                            label = labels.saveInfo,
                            icon = Icons.Default.Folder,
                            onClick = onOpenSaveInfo,
                            enabled = saveInfoEnabled,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    }
                },
            )
        }
    }
}
