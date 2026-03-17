package com.dudoziworkshop.dzlog.ui.camera

import android.content.Context
import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 촬영 성공 피드백 전용 유틸.
 *
 * 정책 변경:
 * - 기존 Compose haptic 대신 기기 진동(짧고 명확한 패턴) + 셔터 사운드를 사용한다.
 * - 실패해도 촬영 플로우를 막지 않도록 모든 동작은 runCatching으로 보호한다.
 */
internal class CaptureFeedback(private val context: Context) {
    private val actionSound: MediaActionSound by lazy {
        MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }
    }

    fun play(successVibrationEnabled: Boolean, soundEnabled: Boolean) {
        if (successVibrationEnabled) {
            runCatching { vibrateSuccessPattern() }
        }
        if (soundEnabled) {
            runCatching { actionSound.play(MediaActionSound.SHUTTER_CLICK) }
        }
    }

    private fun vibrateSuccessPattern() {
        val vibrator = resolveVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        // UX: 짧고 강하게 2회(디-딩). 너무 짧으면 체감이 약해서 35~40ms로 유지한다.
        val timings = longArrayOf(0, 38, 55, 38)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, intArrayOf(0, 255, 0, 255), -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
