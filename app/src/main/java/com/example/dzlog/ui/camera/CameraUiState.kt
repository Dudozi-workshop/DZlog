package com.example.dzlog.ui.camera

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.ui.camera.preview.TapFocusUiState
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

@Stable
internal class CameraPrefsState {
    var captureAspect by mutableStateOf(CaptureAspect.R3_4)
    var saveMode by mutableStateOf(SaveMode.BOTH)
    var continuousPreviewMode by mutableStateOf(ContinuousPreviewMode.OFF)
    var counterDigits by mutableIntStateOf(COUNTER_DIGITS_DEFAULT)
    var showGrid by mutableStateOf(false)
    var zoomRatioTenths by mutableIntStateOf(10)

    var showWmPreview by mutableStateOf(true)
    var wmTableAnchor by mutableStateOf(WatermarkTableAnchor.BOTTOM_RIGHT)
    var wmTableWidthRatio by mutableIntStateOf(40)
    var wmTableHeightRatio by mutableIntStateOf(20)
    var wmOffsetXRatio by mutableIntStateOf(0)
    var wmOffsetYRatio by mutableIntStateOf(0)
    var wmBgAlpha by mutableIntStateOf(80)
    // 0=BLACK, 1=WHITE, 2=TRANSPARENT
    var wmBgStyle by mutableIntStateOf(0)
    var wmLabelScale by mutableIntStateOf(100)
    var wmValueScale by mutableIntStateOf(100)
}

@Stable
internal class CaptureUiState {
    var capturedUri by mutableStateOf<Uri?>(null)
    var isCapturing by mutableStateOf(false)
    val captureGate: AtomicBoolean = AtomicBoolean(false)

    var tapFocusUi by mutableStateOf<TapFocusUiState?>(null)
    var now by mutableStateOf(Date())
    var actualZoomTenths by mutableIntStateOf(10)
    var usableTopRatio by mutableStateOf(0f)
    var usableBottomRatio by mutableStateOf(1f)
}

@Stable
internal class CounterScopeState {
    var scopeNextCounter by mutableIntStateOf(1)
    var lastScopeSnapshot by mutableStateOf<CounterScopeSnapshot?>(null)
}

@Stable
internal class CameraUiState {
    val prefs: CameraPrefsState = CameraPrefsState()
    val capture: CaptureUiState = CaptureUiState()
    val counter: CounterScopeState = CounterScopeState()

    var showWizard by mutableStateOf(false)
}
