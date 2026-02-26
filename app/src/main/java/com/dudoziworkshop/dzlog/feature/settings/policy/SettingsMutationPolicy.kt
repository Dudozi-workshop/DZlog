package com.dudoziworkshop.dzlog.feature.settings.policy

import android.content.Context
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode

suspend fun updateSaveMode(context: Context, mode: SaveMode): String {
    AppSettingsStore.setSaveMode(context, mode)
    return "저장 대상: ${mode.name}"
}

suspend fun updateContinuousPreviewMode(context: Context, mode: ContinuousPreviewMode): String {
    AppSettingsStore.setContinuousPreviewMode(context, mode)
    return "미리보기: ${mode.name}"
}

suspend fun updatePhotoQualityMode(context: Context, mode: PhotoQualityMode): String {
    AppSettingsStore.setPhotoQualityMode(context, mode)
    return "사진 품질: ${mode.label}"
}

suspend fun updateCounterPadding(context: Context, digits: Int): String {
    AppSettingsStore.setCounterPadding(context, digits)
    return if (digits == 0) "카운터 패딩: 없음" else "카운터 자릿수: $digits"
}

suspend fun updateIncludePathInCounterScope(context: Context, enabled: Boolean): String {
    AppSettingsStore.setIncludePathInCounterScope(context, enabled)
    return "카운터 범위에 저장경로 반영: ${if (enabled) "ON" else "OFF"}"
}

suspend fun updateIncludeFilenameInCounterScope(context: Context, enabled: Boolean): String {
    AppSettingsStore.setIncludeFilenameInCounterScope(context, enabled)
    return "카운터 범위에 파일명 반영: ${if (enabled) "ON" else "OFF"}"
}

suspend fun updateHapticEnabled(context: Context, enabled: Boolean): String {
    AppSettingsStore.setHapticEnabled(context, enabled)
    return "진동: ${if (enabled) "ON" else "OFF"}"
}

suspend fun updateCaptureHapticEnabled(context: Context, enabled: Boolean): String {
    AppSettingsStore.setCaptureHapticEnabled(context, enabled)
    return "촬영 진동: ${if (enabled) "ON" else "OFF"}"
}

suspend fun updateBlankWarningEnabled(context: Context, enabled: Boolean): String {
    AppSettingsStore.setBlankWarningEnabled(context, enabled)
    return "공백 경고: ${if (enabled) "ON" else "OFF"}"
}

suspend fun updateToastEnabled(context: Context, enabled: Boolean): String? {
    AppSettingsStore.setToastEnabled(context, enabled)
    return if (enabled) "토스트 피드백 ON" else null
}
