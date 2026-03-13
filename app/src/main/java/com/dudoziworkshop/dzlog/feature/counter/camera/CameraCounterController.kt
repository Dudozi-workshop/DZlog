package com.dudoziworkshop.dzlog.feature.counter.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.ui.camera.CameraUiState

@Composable
internal fun CameraCounterController(
    counterScope: CounterScope,
    scanPrefix: String,
    resumeTick: Int,
    latestCounterEvent: CameraCounterSyncEvent?,
    counterEventTick: Int,
    isTemplateReady: Boolean,
    appSettings: AppSettings,
    ui: CameraUiState,
) {
    val context = LocalContext.current

    // CameraScreen의 counter wiring을 분리: Controller는 Facade 생성 + SyncEffect 연결만 담당한다.
    val counterFacade = remember(context, ui.prefs.counterDigits) {
        CounterFacade(
            context = context,
            counterDigits = ui.prefs.counterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        )
    }

    CameraCounterSyncEffect(
        counterScope = counterScope,
        scanPrefix = scanPrefix,
        resumeTick = resumeTick,
        counterEventTick = counterEventTick,
        latestCounterEvent = latestCounterEvent,
        isTemplateReady = isTemplateReady,
        appSettings = appSettings,
        ui = ui,
        counterFacade = counterFacade,
    )
}
