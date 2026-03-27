package com.dudoziworkshop.dzlog.feature.settings.policy

import android.content.Context
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction

sealed interface SettingsAction {
    data class SaveModeChanged(val mode: SaveMode) : SettingsAction
    data class ContinuousPreviewModeChanged(val mode: ContinuousPreviewMode) : SettingsAction
    data class PhotoQualityModeChanged(val mode: PhotoQualityMode) : SettingsAction
    data class CounterPaddingChanged(val digits: Int) : SettingsAction
    data class IncludePathInCounterScopeChanged(val enabled: Boolean) : SettingsAction
    data class IncludeFilenameInCounterScopeChanged(val enabled: Boolean) : SettingsAction
    data class ToastEnabledChanged(val enabled: Boolean) : SettingsAction
    data class HapticEnabledChanged(val enabled: Boolean) : SettingsAction
    data class CaptureHapticEnabledChanged(val enabled: Boolean) : SettingsAction
    data class CaptureSoundEnabledChanged(val enabled: Boolean) : SettingsAction
    data class VolumeKeyActionChanged(val action: VolumeKeyAction) : SettingsAction
    data class BlankWarningEnabledChanged(val enabled: Boolean) : SettingsAction
    data class AssistShutterEnabledChanged(val enabled: Boolean) : SettingsAction
}

suspend fun applySettingsAction(context: Context, action: SettingsAction): String? {
    return when (action) {
        is SettingsAction.SaveModeChanged -> updateSaveMode(context, action.mode)
        is SettingsAction.ContinuousPreviewModeChanged -> updateContinuousPreviewMode(context, action.mode)
        is SettingsAction.PhotoQualityModeChanged -> updatePhotoQualityMode(context, action.mode)
        is SettingsAction.CounterPaddingChanged -> updateCounterPadding(context, action.digits)
        is SettingsAction.IncludePathInCounterScopeChanged -> updateIncludePathInCounterScope(context, action.enabled)
        is SettingsAction.IncludeFilenameInCounterScopeChanged -> updateIncludeFilenameInCounterScope(context, action.enabled)
        is SettingsAction.ToastEnabledChanged -> updateToastEnabled(context, action.enabled)
        is SettingsAction.HapticEnabledChanged -> updateHapticEnabled(context, action.enabled)
        is SettingsAction.CaptureHapticEnabledChanged -> updateCaptureHapticEnabled(context, action.enabled)
        is SettingsAction.CaptureSoundEnabledChanged -> updateCaptureSoundEnabled(context, action.enabled)
        is SettingsAction.VolumeKeyActionChanged -> updateVolumeKeyAction(context, action.action)
        is SettingsAction.BlankWarningEnabledChanged -> updateBlankWarningEnabled(context, action.enabled)
        is SettingsAction.AssistShutterEnabledChanged -> updateAssistShutterEnabled(context, action.enabled)
    }
}
