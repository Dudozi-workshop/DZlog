package com.dudoziworkshop.dzlog.ui.camera.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dudoziworkshop.dzlog.feature.counter.camera.CameraCounterSyncEvent

internal class CameraViewModel : ViewModel() {
    val ui: CameraUiState = CameraUiState()

    var phraseProgressCounter by mutableIntStateOf(1)
        private set

    var counterEventTick by mutableIntStateOf(0)
        private set

    var latestCounterEvent by mutableStateOf<CameraCounterSyncEvent?>(null)
        private set

    var resumeResyncTick by mutableIntStateOf(0)
        private set

    fun bumpResumeResyncTick() {
        resumeResyncTick += 1
    }

    fun onCaptureCommitted() {
        latestCounterEvent = CameraCounterSyncEvent.CAPTURE_COMMITTED
        counterEventTick += 1
    }

    fun onUndoCommitted() {
        latestCounterEvent = CameraCounterSyncEvent.UNDO_COMMITTED
        counterEventTick += 1
    }

    fun advancePhraseProgress(nextCursor: Int) {
        phraseProgressCounter = nextCursor.coerceAtLeast(1)
    }
}

