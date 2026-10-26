package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice
import kotlinx.coroutines.flow.MutableStateFlow

/** Distinguishes a genuine required-shutter answer from an API query failure. */
internal enum class ShutterRequirementStatus { REQUIRED, NOT_REQUIRED, QUERY_FAILED }

internal data class ShutterRequirementProbe(
    val status: ShutterRequirementStatus,
    val errorType: String? = null,
) {
    val requiresSystemShutter: Boolean get() = status != ShutterRequirementStatus.NOT_REQUIRED
}

internal fun classifyShutterRequirement(result: Result<Boolean>): ShutterRequirementProbe =
    result.fold(
        onSuccess = { required ->
            ShutterRequirementProbe(
                if (required) ShutterRequirementStatus.REQUIRED
                else ShutterRequirementStatus.NOT_REQUIRED,
            )
        },
        onFailure = { error ->
            ShutterRequirementProbe(
                ShutterRequirementStatus.QUERY_FAILED,
                error.javaClass.simpleName.ifEmpty { error.javaClass.name },
            )
        },
    )

internal data class CaptureSoundDiagnosticSnapshot(
    val requirement: ShutterRequirementProbe,
    val enabled: Boolean,
    val selected: CaptureSoundChoice,
    val volume: Int,
    val route: CaptureSoundOutput,
    /** The playback API accepted a call; this does not prove audible output. */
    val playbackStarted: Boolean?,
)

internal object CaptureSoundDiagnostics {
    val lastExposure = MutableStateFlow<CaptureSoundDiagnosticSnapshot?>(null)
}
