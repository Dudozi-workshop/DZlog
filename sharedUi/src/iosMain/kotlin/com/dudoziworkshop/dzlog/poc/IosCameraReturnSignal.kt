package com.dudoziworkshop.dzlog.poc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Notifies the preserved iOS home composition of a native camera return.
 * This refreshes recent media only; it does not change counters or table state.
 */
internal object IosCameraReturnSignal {
    var revision by mutableIntStateOf(0)
        private set

    fun noteCameraClosed() {
        revision += 1
    }
}
