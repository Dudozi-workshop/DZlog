package com.dudoziworkshop.dzlog.ui.camera

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.dudoziworkshop.dzlog.R
import com.dudoziworkshop.dzlog.data.datastore.CaptureSoundChoice
import com.dudoziworkshop.dzlog.data.datastore.CaptureVibrationStrength
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureSoundOutput
import com.dudoziworkshop.dzlog.feature.capture.policy.captureSoundOutput
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
                // Match preview playback and the user's media-volume controls.
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
            CaptureSoundChoice.SYSTEM to soundPool.load(appContext, R.raw.dzlog_default_soft, 1),
            CaptureSoundChoice.SOFT_CONTAREX to soundPool.load(appContext, R.raw.dzlog_contarex_soft, 1),
            CaptureSoundChoice.CLASSIC_WERRA to soundPool.load(appContext, R.raw.dzlog_werra_classic, 1),
            CaptureSoundChoice.DIGITAL_DSLR to soundPool.load(appContext, R.raw.dzlog_dslr_digital, 1),
            CaptureSoundChoice.PPUPPYU to soundPool.load(appContext, R.raw.dzlog_signature_ppuppyu, 1),
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
        // Never alter system volume or mute a mandatory platform shutter.
        // Every optional DZlog sound uses the same gain-adjustable media stream.
        when (captureSoundOutput(
            enabled = soundEnabled,
            selected = soundChoice,
            platformShutterRequired = isPlatformShutterRequired(),
        )) {
            CaptureSoundOutput.OFF -> Unit
            CaptureSoundOutput.SYSTEM -> {
                runCatching { systemSound.play(MediaActionSound.SHUTTER_CLICK) }
            }
            CaptureSoundOutput.CUSTOM -> {
                // A not-yet-loaded or failed custom sample must not trigger a
                // second, much louder platform shutter in its place.
                runCatching { playCustom(soundChoice, soundVolume) }
            }
        }
    }

    // Preview is a deliberate foreground action. Unlike SoundPool's asynchronous
    // sample loading at camera startup, MediaPlayer.create prepares the bundled
    // sample before playback, so an immediate first tap will not be discarded.
    private var previewPlayer: MediaPlayer? = null
    private val previewAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun previewResource(choice: CaptureSoundChoice): Int? = when (choice) {
        CaptureSoundChoice.SYSTEM -> R.raw.dzlog_default_soft
        CaptureSoundChoice.SOFT_CONTAREX -> R.raw.dzlog_contarex_soft
        CaptureSoundChoice.CLASSIC_WERRA -> R.raw.dzlog_werra_classic
        CaptureSoundChoice.DIGITAL_DSLR -> R.raw.dzlog_dslr_digital
        CaptureSoundChoice.PPUPPYU -> R.raw.dzlog_signature_ppuppyu
    }

    @Synchronized
    private fun stopPreview() {
        val previous = previewPlayer
        previewPlayer = null
        if (previous != null) {
            runCatching { previous.stop() }
            runCatching { previous.release() }
        }
    }

    /** Preview only: no vibration, capture, counter, or photo save side effects. */
    @Synchronized
    fun preview(choice: CaptureSoundChoice, volume: Int): Boolean = runCatching {
        stopPreview()
        val resource = previewResource(choice) ?: return@runCatching false
        val media = MediaPlayer.create(
            appContext, resource, previewAttributes,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        ) ?: return@runCatching false
        previewPlayer = media
        val gain = volume.coerceIn(0, 100) / 100f * choice.playbackGain
        media.setVolume(gain, gain)
        media.setOnCompletionListener { finished ->
            synchronized(this) {
                if (previewPlayer === finished) previewPlayer = null
            }
            runCatching { finished.release() }
        }
        media.setOnErrorListener { failed, _, _ ->
            synchronized(this) {
                if (previewPlayer === failed) previewPlayer = null
            }
            runCatching { failed.release() }
            true
        }
        media.start()
        true
    }.getOrDefault(false)

    /** Demonstrate only the requested strength; OFF does not vibrate. */
    fun previewVibration(strength: CaptureVibrationStrength): Boolean {
        if (strength == CaptureVibrationStrength.OFF) return true
        return runCatching {
            val device = resolveVibrator()
            if (device == null || !device.hasVibrator()) return@runCatching false
            vibrate(strength)
            true
        }.getOrDefault(false)
    }

    private fun playCustom(choice: CaptureSoundChoice, volume: Int): Boolean =
        playCustomStream(choice, volume) != 0

    private fun playCustomStream(choice: CaptureSoundChoice, volume: Int): Int {
        if (!choice.available) return 0
        val id = soundIds[choice] ?: return 0
        if (id == 0 || id !in ready) return 0
        val gain = volume.coerceIn(0, 100) / 100f * choice.playbackGain
        return soundPool.play(id, gain, gain, 1, 0, 1f)
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

    companion object {
        /**
         * Android 13+ publicly exposes regional shutter requirements. Honor the
         * platform when mandatory; do not attempt a quieter substitute.
         */
        fun isPlatformShutterRequired(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                runCatching { MediaActionSound.mustPlayShutterSound() }.getOrDefault(true)
    }

    fun release() {
        stopPreview()
        runCatching { soundPool.release() }
        runCatching { systemSound.release() }
    }
}
