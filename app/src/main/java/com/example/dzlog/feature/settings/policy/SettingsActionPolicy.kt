package com.example.dzlog.feature.settings.policy

import android.content.Context
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode

sealed interface SettingsAction {
    data class SaveModeChanged(val mode: SaveMode) : SettingsAction
    data class ContinuousPreviewModeChanged(val mode: ContinuousPreviewMode) : SettingsAction
    data class CounterPaddingChanged(val digits: Int) : SettingsAction
    data class IncludePathInCounterScopeChanged(val enabled: Boolean) : SettingsAction
    data class IncludeFilenameInCounterScopeChanged(val enabled: Boolean) : SettingsAction
    data class ToastEnabledChanged(val enabled: Boolean) : SettingsAction
    data class HapticEnabledChanged(val enabled: Boolean) : SettingsAction
    data class BlankWarningEnabledChanged(val enabled: Boolean) : SettingsAction
}

suspend fun applySettingsAction(context: Context, action: SettingsAction): String? {
    return when (action) {
        is SettingsAction.SaveModeChanged -> updateSaveMode(context, action.mode)
        is SettingsAction.ContinuousPreviewModeChanged -> updateContinuousPreviewMode(context, action.mode)
        is SettingsAction.CounterPaddingChanged -> updateCounterPadding(context, action.digits)
        is SettingsAction.IncludePathInCounterScopeChanged -> updateIncludePathInCounterScope(context, action.enabled)
        is SettingsAction.IncludeFilenameInCounterScopeChanged -> updateIncludeFilenameInCounterScope(context, action.enabled)
        is SettingsAction.ToastEnabledChanged -> updateToastEnabled(context, action.enabled)
        is SettingsAction.HapticEnabledChanged -> updateHapticEnabled(context, action.enabled)
        is SettingsAction.BlankWarningEnabledChanged -> updateBlankWarningEnabled(context, action.enabled)
    }
}
