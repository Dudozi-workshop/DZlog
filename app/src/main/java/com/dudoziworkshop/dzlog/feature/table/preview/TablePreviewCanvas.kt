package com.dudoziworkshop.dzlog.feature.table.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.feature.table.render.TableRender
import java.util.Date

@Composable
fun TablePreviewCanvas(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier,
    wmBgStyle: Int = 0,
    wmBgAlpha: Int = 80,
    wmValueScale: Int = 100,
    wmTextColorMode: Int = 0,
    wmManualTextColor: Int = 1,
    wmTextAlign: Int = 0,
    tableDetailGridEnabled: Boolean = true,
    wmWidthRatio: Int = 40,
    wmHeightRatio: Int = 20,
    overlay: (@Composable () -> Unit)? = null,
) {
    val frameAspectRatio = TableLayoutCalculator.defaultAspectRatio

    TablePreviewFrame(
        aspectRatio = frameAspectRatio,
        modifier = modifier
    ) { innerModifier ->
        TableRender(
            templateState = templateState,
            counterDigits = counterDigits,
            now = now,
            bgStyle = wmBgStyle.coerceIn(0, 2),
            bgAlpha = wmBgAlpha.coerceIn(0, 255),
            valueScale = wmValueScale.coerceIn(60, 160),
            textColorMode = wmTextColorMode,
            manualTextColor = wmManualTextColor,
            textAlign = wmTextAlign,
            gridEnabled = tableDetailGridEnabled,
            tableWidthRatio = wmWidthRatio,
            tableHeightRatio = wmHeightRatio,
            modifier = innerModifier
                .fillMaxSize()
                .padding(4.dp),
        )
        overlay?.invoke()
    }
}

@Composable
fun TablePreview(
    templateState: TableTemplateState,
    counterDigits: Int,
    now: Date,
    modifier: Modifier = Modifier,
    wmBgStyle: Int = 0,
    wmBgAlpha: Int = 80,
    wmValueScale: Int = 100,
    wmTextColorMode: Int = 0,
    wmManualTextColor: Int = 1,
    wmTextAlign: Int = 0,
    tableDetailGridEnabled: Boolean = true,
    wmWidthRatio: Int = 40,
    wmHeightRatio: Int = 20,
    overlay: (@Composable () -> Unit)? = null,
) {
    TablePreviewCanvas(
        templateState = templateState,
        counterDigits = counterDigits,
        now = now,
        modifier = modifier,
        wmBgStyle = wmBgStyle,
        wmBgAlpha = wmBgAlpha,
        wmValueScale = wmValueScale,
        wmTextColorMode = wmTextColorMode,
        wmManualTextColor = wmManualTextColor,
        wmTextAlign = wmTextAlign,
        tableDetailGridEnabled = tableDetailGridEnabled,
        wmWidthRatio = wmWidthRatio,
        wmHeightRatio = wmHeightRatio,
        overlay = overlay,
    )
}
