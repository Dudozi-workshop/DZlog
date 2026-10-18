package com.dudoziworkshop.dzlog.ui.camera

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice
import com.dudoziworkshop.dzlog.data.datastore.CaptureVibrationStrength
import java.util.concurrent.ConcurrentHashMap

/**
 * CameraX onCaptureStarted is the only exposure feedback trigger.
 * No counter, image, storage, or Undo state is accessed here.
 */
internal class CaptureFeedback(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private val soundIds: Map<CaptureSoundChoice, Int>
    private val systemSound = MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }

    init {
        // Register the listener before loading the first sample.
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) ready.add(sampleId)
        }
        soundIds = mapOf(
            CaptureSoundChoice.SOFT_CONTAREX to soundPool.load(appContext, R.raw.dzlog_contarex_soft, 1),
            CaptureSoundChoice.CLASSIC_WERRA to soundPool.load(appContext, R.raw.dzlog_werra_classic, 1),
            CaptureSoundChoice.DIGITAL_DSLR to soundPool.load(appContext, R.raw.dzlog_dslr_digital, 1),
        )
    }

    fun play(
        successVibrationEnabled: Boolean,
        soundEnabled: Boolean,
        soundChoice: CaptureSoundChoice = CaptureSoundChoice.SYSTEM,
        soundVolume: Int = 70,
        vibrationStrength: CaptureVibrationStrength = CaptureVibrationStrength.MEDIUM,
    ) {
        if (successVibrationEnabled && vibrationStrength != CaptureVibrationStrength.OFF) {
            runCatching { vibrate(vibrationStrength) }
        }
        // Respect device / regional requirements even when the app sound toggle is off.
        // Forced-shutter devices use the mandatory system sound, not a custom replacement.
        val forced = runCatching {
            (appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
                ?.isCameraSoundForced() == true
        }.getOrDefault(false)
        if (forced || soundEnabled) {
            runCatching {
                if (forced || soundChoice == CaptureSoundChoice.SYSTEM ||
                    !playCustom(soundChoice, soundVolume)) {
                    systemSound.play(MediaActionSound.SHUTTER_CLICK)
                }
            }
        }
    }

    /** Never triggers a capture, vibration, counter increment, or photo save. */
    fun preview(choice: CaptureSoundChoice, volume: Int): Boolean =
        runCatching {
            when (choice) {
                CaptureSoundChoice.SYSTEM -> {
                    systemSound.play(MediaActionSound.SHUTTER_CLICK)
                    true
                }
                else -> playCustom(choice, volume)
            }
        }.getOrDefault(false)

    private fun playCustom(choice: CaptureSoundChoice, volume: Int): Boolean {
        if (!choice.available) return false
        val id = soundIds[choice] ?: return false
        if (id == 0 || id !in ready) return false
        val gain = (volume.coerceIn(0, 100) / 100f)
        return soundPool.play(id, gain, gain, 1, 0, 1f) != 0
    }

    private fun vibrate(strength: CaptureVibrationStrength) {
        val vibrator = resolveVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        val duration = when (strength) {
            CaptureVibrationStrength.OFF -> return
            CaptureVibrationStrength.LOW -> 20L
            CaptureVibrationStrength.MEDIUM -> 32L
            CaptureVibrationStrength.HIGH -> 45L
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = if (!vibrator.hasAmplitudeControl()) {
                VibrationEffect.DEFAULT_AMPLITUDE
            } else when (strength) {
                CaptureVibrationStrength.LOW -> 85
                CaptureVibrationStrength.MEDIUM -> 190
                CaptureVibrationStrength.HIGH -> 255
                CaptureVibrationStrength.OFF -> return
            }
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun resolveVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun release() {
        runCatching { soundPool.release() }
        runCatching { systemSound.release() }
    }
}
