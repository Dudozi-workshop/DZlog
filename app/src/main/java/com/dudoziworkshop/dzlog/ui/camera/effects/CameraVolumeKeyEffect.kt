package com.dudoziworkshop.dzlog.ui.camera.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
import com.dudoziworkshop.dzlog.ui.camera.VolumeKeyInputBus
import com.dudoziworkshop.dzlog.ui.camera.VolumeKeyPress

@Composable
internal fun CameraVolumeKeyEffect(
    volumeKeyAction: VolumeKeyAction,
    onCapture: () -> Unit,
    onZoomDelta: (deltaTenths: Int) -> Unit,
) {
    val latestAction = rememberUpdatedState(volumeKeyAction)
    val latestOnCapture = rememberUpdatedState(onCapture)
    val latestOnZoomDelta = rememberUpdatedState(onZoomDelta)

    DisposableEffect(volumeKeyAction) {
        VolumeKeyInputBus.setVolumeKeyAction(volumeKeyAction)
        onDispose { VolumeKeyInputBus.setVolumeKeyAction(VolumeKeyAction.NONE) }
    }

    LaunchedEffect(Unit) {
        VolumeKeyInputBus.events.collect { press ->
            when (latestAction.value) {
                VolumeKeyAction.CAPTURE -> latestOnCapture.value()
                VolumeKeyAction.ZOOM -> {
                    val delta = if (press == VolumeKeyPress.UP) 1 else -1
                    latestOnZoomDelta.value(delta)
                }
                VolumeKeyAction.NONE -> Unit
            }
        }
    }
}

