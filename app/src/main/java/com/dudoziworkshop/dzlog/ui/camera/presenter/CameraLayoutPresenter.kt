package com.dudoziworkshop.dzlog.ui.camera.presenter

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

internal data class CameraLayoutState(
    val safeTopY: Float?,
    val safeBottomY: Float?,
    val usableVerticalMarginPx: Float,
    val cameraRootHeightPx: Float,
    val shutterButtonTopY: Float?,
    val onCameraRootHeightPxChange: (Float) -> Unit,
    val onShutterButtonTopYChange: (Float?) -> Unit,
)

@Composable
internal fun rememberCameraLayoutState(): CameraLayoutState {
    var cameraRootHeightPx by remember { mutableFloatStateOf(0f) }
    var shutterButtonTopY by remember { mutableStateOf<Float?>(null) }

    val density = LocalDensity.current
    val safeTopInsetPx = with(density) {
        WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding().toPx()
    }
    val safeBottomInsetPx = with(density) {
        WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding().toPx()
    }

    return CameraLayoutState(
        safeTopY = safeTopInsetPx.takeIf { it > 0f },
        safeBottomY = (cameraRootHeightPx - safeBottomInsetPx).takeIf { cameraRootHeightPx > 0f },
        usableVerticalMarginPx = with(density) { 10.dp.toPx() },
        cameraRootHeightPx = cameraRootHeightPx,
        shutterButtonTopY = shutterButtonTopY,
        onCameraRootHeightPxChange = { cameraRootHeightPx = it },
        onShutterButtonTopYChange = { shutterButtonTopY = it },
    )
}
